package catalog

import java.net.URI

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import akka.pattern.ask
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers

import scala.concurrent.duration._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val uriFormat = new JsonFormat[java.net.URI] {
    override def write(obj: java.net.URI): spray.json.JsValue = JsString(obj.toString)
    override def read(json: JsValue): URI = json match {
      case JsString(url) => new URI(url)
      case _             => throw new RuntimeException("Parsing exception")
    }
  }
  implicit val itemFormat  = jsonFormat5(ProductCatalog.Item)
  implicit val itemsFormat = jsonFormat1(ProductCatalog.Items)
}

class CatalogHttpServer extends HttpApp with JsonSupport {

  implicit val timeout: Timeout = 5 seconds

  val config = ConfigFactory.load()
  val catalogSystem = ActorSystem("catalog", config.getConfig("catalog").withFallback(config))
  val catalogWorkers = catalogSystem.actorOf(Props(new CatalogSupervisor()), "catalogsupervisor")


  override protected def routes: Route = {
    path("search") {
      get {
        parameters('keywords.as(List[String]).?) { keywords =>
          complete {
            (catalog ? CatalogSupervisor.SearchItem(keywords.getOrElse(Seq.empty).toList))
              .mapTo[SearchResult(items)]
          }
        }
      }
    }
  }

  //  Await.result(catalogSystem.whenTerminated, Duration.Inf)
}
