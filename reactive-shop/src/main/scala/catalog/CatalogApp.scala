package catalog

import akka.actor.{ActorSystem, PoisonPill, Props}
import com.typesafe.config.ConfigFactory

object CatalogApp extends App {

  val config = ConfigFactory.load("catalog_application.conf")
  val system = ActorSystem("catalog", config)


  val catalog = system.actorOf(Props(new CatalogSupervisor()), "catalogSup")
  println(system.child("catalogSup"))

  val input = scala.io.StdIn
  var run = true

  catalog ! "DUPA"

  while (run) {
    val str = input.readLine("Enter command : [quit]\n")
    str match {
      case "quit" =>
        run = false
        catalog ! PoisonPill

      case _ =>
        println("Invalid command")
    }
  }
  system.terminate()
}
