import Cart.AddItem
import akka.actor.{ActorSystem, Props}
import Checkout._

object ReactiveShop extends App{
  val system = ActorSystem("system")

  val cart = system.actorOf(Props(new Cart()))
  val checkout = system.actorOf(Props(new Checkout()))

  cart ! AddItem
  cart ! AddItem

  checkout ! SelectDeliveryMethod("inpost")
  checkout ! SelectPaymentMethod("visa")
  checkout ! PaymentReceived
  checkout ! Cancelled
}
