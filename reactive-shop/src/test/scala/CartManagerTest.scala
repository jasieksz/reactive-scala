import java.net.URI

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import managers.{CartManager, OrderManager}
import managers.CartManager._
import managers.OrderManager.GetCart
import model.{Cart, Item}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


class CartManagerTest extends TestKit(ActorSystem("CartManagerTest"))
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender
  with ScalaFutures
  with Matchers {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(5000, Seconds))

  val apple: Item = Item(URI.create("apple"), "apple", 1)
  val orange: Item = Item(URI.create("orange"), "orange", 1)

  "A cart" should {
    "add item" in {
      val probe = TestProbe()
      val actorRef = system.actorOf(Props(new CartManager("id-2")))
      actorRef ! StartCart(actorRef)

      actorRef ! AddItem(apple, probe.ref)
      probe.expectMsg(CartManager.ItemAdded(apple))
    }

    "remove item" in {
      val probe = TestProbe()
      val actorRef = system.actorOf(Props(new CartManager("id-3")))

      actorRef ! StartCart(actorRef)
      actorRef ! AddItem(apple, actorRef)
      actorRef ! RemoveItem(apple, 1, actorRef)

      actorRef ! OrderManager.GetCart(probe.ref)

      probe.expectMsg(Cart.empty)
    }

    "start checkout" in {
      val probe = TestProbe()
      val actorRef = system.actorOf(Props(new CartManager("id-4")))
      actorRef ! StartCart(actorRef)
      actorRef ! AddItem(apple, actorRef)

      actorRef ! StartCheckout(probe.ref)
      probe.expectMsgType[CartManager.CheckoutStarted]
    }

    "persist" in {
      val managerRef = TestProbe()
      val cart = system.actorOf(Props(new CartManager("id-6")))
      var cartData = Cart.empty

      cart ! StartCart(managerRef.ref)
      managerRef.expectMsg(CartStarted(cart))

      cart ! GetCart(managerRef.ref)
      managerRef.expectMsg(cartData)

      cart ! AddItem(apple, managerRef.ref)
      cartData = cartData.addItem(apple)
      managerRef.expectMsg(ItemAdded(apple))

      cart ! GetCart(managerRef.ref)
      managerRef.expectMsg(cartData)

      cart ! AddItem(orange, managerRef.ref)
      cartData = cartData.addItem(orange)
      managerRef.expectMsg(ItemAdded(orange))

      cart ! GetCart(managerRef.ref)
      managerRef.expectMsg(cartData)

      cart ! "snap"

      Thread.sleep(3000)

      cart ! PoisonPill

      val restoredCart = system.actorOf(Props(new CartManager("id-6")))

      restoredCart ! GetCart(managerRef.ref)
      managerRef.expectMsg(cartData)

      restoredCart ! AddItem(apple, managerRef.ref)
      cartData = cartData.addItem(apple)
      managerRef.expectMsg(ItemAdded(apple))

      restoredCart ! GetCart(managerRef.ref)
      managerRef.expectMsg(cartData)

      restoredCart ! RemoveItem(orange, 1, managerRef.ref)
      cartData = cartData.removeItem(orange)
      managerRef.expectMsg(ItemRemoved(orange, 1))

      restoredCart ! GetCart(managerRef.ref)
      managerRef.expectMsg(cartData)
    }
  }
}
