import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.TestProbe
import akka.util.Timeout
import catalog.CatalogSupervisor.{SearchItem, SearchResult}
import catalog.{Catalog, SearchWorker}
import com.typesafe.config.ConfigFactory
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.duration._


class CatalogSupervisorTest extends AsyncFlatSpec with Matchers {

  implicit val timeout: Timeout = 3.second

  "A catalog supervisor" should
    "find items" in {
    val config = ConfigFactory.load()

    val catalogSystem = ActorSystem("Catalog", config.getConfig("catalog").withFallback(config))
    catalogSystem.actorOf(Props(new SearchWorker(new Catalog())), "searchworker")

    val actorSystem = ActorSystem("system")
    val catalogSupervisor = actorSystem.actorSelection("akka.tcp://Catalog@127.0.0.1:2554/user/searchworker")

    val probe = TestProbe()(actorSystem)
    val query = SearchItem(List("Fanta"), probe.ref)

    for {
      productCatalogActorRef <- catalogSupervisor.resolveOne()
      items <- (productCatalogActorRef ? query).mapTo[SearchResult]
    } yield {
      println(items)
      assert(items.items.size == 10)
    }

  }
}
