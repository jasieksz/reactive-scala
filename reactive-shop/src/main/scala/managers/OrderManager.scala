package managers

import akka.actor.{ActorRef, PoisonPill, Props, Timers}
import akka.event.LoggingReceive
import akka.util.Timeout
import managers.OrderManager._
import model.Item
import akka.pattern.ask
import catalog.CatalogSupervisor
import catalog.CatalogSupervisor.SearchResult

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


class OrderManager extends Timers {
  implicit val timeout: Timeout = Timeout(1 seconds)

  val productCatalog = context.actorSelection("akka.tcp://catalog@127.0.0.1:2554/catalog/user/catalogsupervisor")

  self ! Initialize

  override def receive: Receive = uninitialized()

  def uninitialized(): Receive = LoggingReceive {
    case Initialize =>
      val cart = context.actorOf(Props(new CartManager("id-1")))
      cart ! CartManager.StartCart(self)

    case CartManager.CartStarted(cart) =>
      context.become(withCart(cart))

    case CheckoutManager.CheckoutClosed =>
      self ! Initialize
  }

  def withCart(cart: ActorRef): Receive = LoggingReceive {
    case AddItem(item) =>
      cart ! CartManager.AddItem(item, self)

    case RemoveItem(item, count) =>
      cart ! CartManager.RemoveItem(item, count, self)

    case StartCheckout =>
      cart ! CartManager.StartCheckout(self)

    case CartManager.CheckoutStarted(checkout) =>
      context.become(withCheckout(cart, checkout))

    case SearchItem(keyWords) =>
      println(productCatalog.anchorPath)
      productCatalog ! CatalogSupervisor.SearchItem(keyWords, self)
      for {
        productCatalogActorRef <- productCatalog.resolveOne()
        items <- (productCatalogActorRef ? CatalogSupervisor.SearchItem(keyWords, self)).mapTo[SearchResult]
      } yield {
        println(items)
      }
  }

  def withCheckout(cart: ActorRef, checkout: ActorRef): Receive = LoggingReceive {
    case SelectDeliveryMethod(method) =>
      checkout ! CheckoutManager.SelectDeliveryMethod(method, self)

    case SelectPaymentMethod(method) =>
      checkout ! CheckoutManager.SelectPaymentMethod(method, self)

    case CancelCheckout =>
      checkout ! CancelCheckout

    case CheckoutManager.CheckoutCancelled(cart) =>
      context.become(withCart(cart))

    case Buy =>
      checkout ! CheckoutManager.Buy(self)

    case CheckoutManager.PaymentServiceStarted(payment) =>
      context.become(withPayment(cart, payment))
  }

  def withPayment(cart: ActorRef, payment: ActorRef): Receive = LoggingReceive {

    case Pay =>
      payment ! PaymentManager.Pay(self)

    case PaymentManager.PaymentConfirmed(_) =>
      cart ! PoisonPill
      context.become(uninitialized())

    case CancelPayment =>
      payment ! PaymentManager.Cancel(self)

    case PaymentManager.PaymentCancelled(checkout) =>
      context.become(withCheckout(cart, checkout))
  }
}

object OrderManager {

  sealed trait OrderManagerCommand extends Command

  case class AddItem(item: Item) extends OrderManagerCommand

  case class RemoveItem(item: Item, count: Int) extends OrderManagerCommand

  case class SelectDeliveryMethod(method: String) extends OrderManagerCommand

  case class SelectPaymentMethod(method: String) extends OrderManagerCommand

  case object Buy extends OrderManagerCommand

  case object Pay extends OrderManagerCommand

  case object Initialize extends OrderManagerCommand

  case object StartCheckout extends OrderManagerCommand

  case object CancelCheckout extends OrderManagerCommand

  case object CancelPayment extends OrderManagerCommand

  case class GetCart(replyTo: ActorRef) extends OrderManagerCommand

  case class SearchItem(keyWords: List[String]) extends OrderManagerCommand

  sealed trait OrderManagerEvent extends Event

  case object Done extends OrderManagerEvent

}