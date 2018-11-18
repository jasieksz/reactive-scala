import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import catalog.CatalogSupervisor
import catalog.CatalogSupervisor.{SearchItem, SearchResult}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


class CatalogSupervisorTest extends TestKit(ActorSystem("CatalogSupervisorTest"))
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender
  with ScalaFutures
  with Matchers {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(5000, Seconds))

  "A catalog" should {
    "add item" in {
      val probe = TestProbe()
    }

  }
}
