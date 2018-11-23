package catalog

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object CatalogApp extends App {

  val config = ConfigFactory.load()
  val catalogSystem = ActorSystem("catalog", config.getConfig("catalog").withFallback(config))

  val catalog = catalogSystem.actorOf(Props(new CatalogSupervisor()), "catalogsupervisor")

  println(catalog.path)
  Await.result(catalogSystem.whenTerminated, Duration.Inf)
}
