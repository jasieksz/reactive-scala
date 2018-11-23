import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import managers.PaymentManager
import managers.PaymentManager.Pay
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class PaymentManagerTest extends TestKit(ActorSystem("PaymentManagerTest"))
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender
  with ScalaFutures
  with Matchers {


//  ??? // TODO : Mock http
  "A payment manager" should {
    "receive payment" in {
      val checkoutProbe = TestProbe()
      val orderProbe = TestProbe()
      val paymentManager = system.actorOf(Props(new PaymentManager(checkoutProbe.ref)))

      paymentManager ! Pay(orderProbe.ref)

      Thread.sleep(5000)
    }
  }
}
