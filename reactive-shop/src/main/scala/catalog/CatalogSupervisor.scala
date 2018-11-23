package catalog

import java.io.IOException

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.event.LoggingReceive
import akka.routing.RoundRobinPool
import catalog.CatalogSupervisor.SearchItem
import managers.{Command, Event}
import model.Item

import scala.concurrent.TimeoutException
import scala.concurrent.duration._

class CatalogSupervisor extends Actor {

  val catalog: Catalog = new Catalog()
  val router: ActorRef = context.actorOf(RoundRobinPool(4).props(Props(new SearchWorker(catalog))), "router")

  override def receive: Receive =  LoggingReceive {
    case SearchItem(keyWords, replyTo) =>
      router.forward(SearchItem(keyWords, replyTo))
  }

  override def supervisorStrategy: OneForOneStrategy = OneForOneStrategy(10, 1.minute) {
    case _: IOException => Restart
    case _: TimeoutException => Restart
    case _: Exception => Escalate
  }
}

object CatalogSupervisor {

  sealed trait CatalogCommand extends Command

  case class SearchItem(keyWords: List[String], replyTo: ActorRef) extends CatalogCommand

  case class GetItem(item: Item, quantity: Int, replyTo: ActorRef) extends CatalogCommand

  sealed trait CatalogEvent extends Event

  case class SearchResult(items: List[Item]) extends CatalogEvent

  case object CatalogOperationSuccess extends CatalogEvent

  case object CatalogOperationFailure extends CatalogEvent

}
