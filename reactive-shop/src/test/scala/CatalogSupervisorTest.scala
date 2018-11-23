import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.TestProbe
import akka.util.Timeout
import catalog.CatalogSupervisor
import catalog.CatalogSupervisor.{SearchItem, SearchResult}
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success


class CatalogSupervisorTest extends FlatSpec with Matchers {

  implicit val timeout: Timeout = 10.second

  "A catalog supervisor" should
    "find items" in {

    val config = ConfigFactory.load()

    val catalogSystem = ActorSystem("Catalog", config.getConfig("catalog").withFallback(config))
    catalogSystem.actorOf(Props(new CatalogSupervisor()), "catalogsupervisor")

    Thread.sleep(5000)

    val shopSystem = ActorSystem("Shop", config.getConfig("shop").withFallback(config))
    val testActor = TestProbe()(shopSystem)
    val catalogSupervisor = shopSystem.actorSelection("akka.tcp://Catalog@127.0.0.1:2554/user/catalogsupervisor")

    val query = SearchItem(List("Fanta Orange"), testActor.ref)

    val catalogSupervisorRef: Future[ActorRef] = catalogSupervisor.resolveOne()

    catalogSupervisorRef.onComplete {
      case Success(ref) =>
        ref ! query
      case scala.util.Failure(exception) =>
        exception.printStackTrace()
    }

    val searchResult: SearchResult = testActor.receiveOne(10 seconds).asInstanceOf[SearchResult]
    assert(searchResult.items.size == 7)
  }
}
