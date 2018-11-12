import java.util.UUID

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import managers.{CheckoutManager, PaymentManager}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class CheckoutManagerTest extends TestKit(ActorSystem("CheckoutManagerTest"))
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender
  with ScalaFutures
  with Matchers {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(5000, Seconds))

  "A checkout" should {
    "close after receiving payment" in {
      val orderManagerProbe = TestProbe()
      val cartManagerProbe = TestProbe()

      val checkout = system.actorOf(Props(new CheckoutManager("test-id-1", cartManagerProbe.ref, orderManagerProbe.ref)))
      checkout ! CheckoutManager.StartCheckout(cartManagerProbe.ref, orderManagerProbe.ref)
      cartManagerProbe.expectMsg(CheckoutManager.CheckoutStarted(checkout, orderManagerProbe.ref))

      checkout ! CheckoutManager.SelectDeliveryMethod("poczta", orderManagerProbe.ref)
      orderManagerProbe.expectMsg(CheckoutManager.DeliveryMethodSelected("poczta"))

      checkout ! "print"
      checkout ! "snap"
      Thread.sleep(3000)
      checkout ! PoisonPill

      val restoredCheckout = system.actorOf(Props(new CheckoutManager("test-id-1", cartManagerProbe.ref, orderManagerProbe.ref)))

      Thread.sleep(2000)


      restoredCheckout ! CheckoutManager.SelectPaymentMethod("visa", orderManagerProbe.ref)
      orderManagerProbe.expectMsg(CheckoutManager.PaymentMethodSelected("visa"))

      restoredCheckout ! CheckoutManager.Buy(orderManagerProbe.ref)
      orderManagerProbe.expectMsgType[CheckoutManager.PaymentServiceStarted]

      restoredCheckout ! PaymentManager.PaymentReceived(UUID.randomUUID())
      cartManagerProbe.expectMsg(CheckoutManager.CheckoutClosed)
      orderManagerProbe.expectMsg(CheckoutManager.CheckoutClosed)
    }
  }
}
