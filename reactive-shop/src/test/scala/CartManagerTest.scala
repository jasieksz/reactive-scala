import java.net.URI

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import managers.{CartManager, OrderManager}
import managers.CartManager.{AddItem, RemoveItem, StartCheckout}
import model.{Cart, Item}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._


class CartManagerTest extends TestKit(ActorSystem("CartManagerTest"))
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender
  with ScalaFutures
  with Matchers {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(5000, Seconds))

  val apple: Item = Item(URI.create("apple"), "apple", 1)

  "A cart" should {
    "add item" in {
      val probe = TestProbe()
      val actorRef = system.actorOf(Props(new CartManager()))

      actorRef ! AddItem(apple, probe.ref)
      probe.expectMsg(CartManager.ItemAdded(apple))
    }

    "remove item" in {
      val probe = TestProbe()
      val actorRef = system.actorOf(Props(new CartManager()))

      actorRef ! AddItem(apple, actorRef)
      actorRef ! RemoveItem(apple, 1, actorRef)

      actorRef ! OrderManager.GetCart(probe.ref)

      probe.expectMsg(Cart.empty)
    }

    "start checkout" in {
      val probe = TestProbe()
      val actorRef = system.actorOf(Props(new CartManager()))
      actorRef ! AddItem(apple, actorRef)

      actorRef ! StartCheckout(probe.ref)
      probe.expectMsgType[CartManager.CheckoutStarted]
    }
  }
}
