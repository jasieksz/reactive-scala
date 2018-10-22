import Cart.{ItemAdded, ItemRemoved}
import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKit}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._


class CartFSMTest extends TestKit(ActorSystem("CartFSMTest")) with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll with Eventually {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 2 seconds)

  "Cart" should "have 2 items" in {
    val cart = TestFSMRef(new CartFSM())
    cart ! ItemAdded
    cart ! ItemAdded
    cart.stateName shouldBe NonEmpty
    cart.stateData shouldBe CartItems(2)
  }

  "Cart" should "ignore removing non existent items" in {
    val cart = TestFSMRef(new CartFSM())
    cart ! ItemRemoved
    cart.stateName shouldBe Empty
    cart.stateData shouldBe CartItems(0)
  }

  "Cart" should "be in Empty state after timeout" in {
    val cart = TestFSMRef(new CartFSM(1 second))
    cart ! ItemAdded
    eventually {
      cart.stateName shouldBe Empty
      cart.stateData shouldBe CartItems()
    }
  }

  "Cart" should "be in NonEmpty state before timeout" in {
    val cart = TestFSMRef(new CartFSM(3 second))
    cart ! ItemAdded
    eventually {
      cart.stateName shouldBe NonEmpty
      cart.stateData shouldBe CartItems(1)
    }
  }
}
