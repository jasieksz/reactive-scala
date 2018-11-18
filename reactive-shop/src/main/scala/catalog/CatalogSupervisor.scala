package catalog

import java.io.{File, IOException}
import java.net.URI

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.event.LoggingReceive
import akka.routing.RoundRobinPool
import catalog.CatalogSupervisor.{CatalogOperationSuccess, GetItem, LookUpItem}
import com.typesafe.config.ConfigFactory
import managers.{Command, Event}
import model.Item

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.io.{BufferedSource, Source}

class CatalogSupervisor extends Actor {

  val router: ActorRef = context.actorOf(RoundRobinPool(4).props(Props[SearchWorker]), "router")

  override def receive: Receive = uninitialized()

  def uninitialized(): Receive = {
    case _ =>
      val bufferedSource: BufferedSource = Source.fromResource("product_db")
      val items: Map[URI, Int] =
        bufferedSource
          .getLines()
          .drop(1)
          .map(line => line.replaceAll("\"", ""))
          .map(line => (URI.create(line.split(",")(0)), 10))
          .toMap

      context.become(initialized(Catalog(items)))
  }

  def initialized(catalog: Catalog): Receive = LoggingReceive {
    case LookUpItem(name, replyTo) =>
      router ! LookUpItem(name, replyTo)

    case GetItem(item, quantity, replyTo) =>
      val result: (Catalog, Boolean) = catalog.removeItem(item.id, quantity)
      if (result._2){
        replyTo ! CatalogOperationSuccess
        context.become(initialized(result._1))
      } // Failure is handled by replyTo

  }

  override def supervisorStrategy: OneForOneStrategy = OneForOneStrategy(10, 1.minute) {
    case _: IOException => Restart
    case _: TimeoutException => Restart
    case _: Exception => Escalate
  }
}

object CatalogSupervisor {

  sealed trait CatalogCommand extends Command

  case class LookUpItem(name: String, replyTo: ActorRef) extends CatalogCommand

  case class GetItem(item: Item, quantity: Int, replyTo: ActorRef) extends CatalogCommand

  sealed trait CatalogEvent extends Event

  case class LookUpResult(items: List[Item]) extends CatalogEvent

  case object CatalogOperationSuccess extends CatalogEvent

  case object CatalogOperationFailure extends CatalogEvent

}

/*
"ean","name","brand"
"0000040822938","Fanta orange","Fanta"
 */