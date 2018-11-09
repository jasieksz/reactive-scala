import java.util.UUID

import akka.actor.{ActorSystem, Props}
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
      val checkout = system.actorOf(Props(new CheckoutManager()))
      checkout ! CheckoutManager.Start(orderManagerProbe.ref, cartManagerProbe.ref)
      cartManagerProbe.expectMsgType[CheckoutManager.Started]

      checkout ! CheckoutManager.SelectDeliveryMethod("poczta", orderManagerProbe.ref)
      orderManagerProbe.expectMsg(CheckoutManager.DeliveryMethodSelected("poczta"))

      checkout ! CheckoutManager.SelectPaymentMethod("visa", orderManagerProbe.ref)
      orderManagerProbe.expectMsg(CheckoutManager.PaymentMethodSelected("visa"))

      checkout ! CheckoutManager.Buy(orderManagerProbe.ref)
      orderManagerProbe.expectMsgType[CheckoutManager.PaymentServiceStarted]

      checkout ! PaymentManager.PaymentReceived(UUID.randomUUID())
      cartManagerProbe.expectMsg(CheckoutManager.Closed)
      orderManagerProbe.expectMsg(CheckoutManager.Closed)
    }
  }
}
