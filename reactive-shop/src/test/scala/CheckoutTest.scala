import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class CheckoutTest extends TestKit(ActorSystem("CheckoutTest"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll with Eventually {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Checkout" should "in uninitialized state" in {
    val checkoutRef = TestActorRef(new Checkout())
    val checkout = checkoutRef.underlyingActor

    val probe = TestProbe()
    probe.send(checkoutRef, Checkout.Start(null))
    expectMsg("")

  }
}