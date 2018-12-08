package catalog

import java.io.IOException

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.event.LoggingReceive
import akka.routing.{DefaultOptimalSizeExploringResizer, RoundRobinPool}
import catalog.CatalogSupervisor.SearchItem
import managers.{Command, Event}
import model.Item

import scala.concurrent.TimeoutException
import scala.concurrent.duration._

class CatalogSupervisor extends Actor {

  val catalog: Catalog = new Catalog()
  val pool: RoundRobinPool = RoundRobinPool(4, Option.apply(DefaultOptimalSizeExploringResizer()))
  val router: ActorRef = context.actorOf(pool.props(Props(new SearchWorker(catalog))), "router")

  override def receive: Receive =  LoggingReceive {
    case SearchItem(keyWords) =>
      router.forward(SearchItem(keyWords))
  }

  override def supervisorStrategy: OneForOneStrategy = OneForOneStrategy(10, 1.minute) {
    case _: IOException => Restart
    case _: TimeoutException => Restart
    case _: Exception => Escalate
  }
}

object CatalogSupervisor {

  sealed trait CatalogCommand extends Command

  case class SearchItem(keyWords: List[String]) extends CatalogCommand

  case class GetItem(item: Item, quantity: Int, replyTo: ActorRef) extends CatalogCommand

  sealed trait CatalogEvent extends Event

  case class SearchResult(items: List[Item]) extends CatalogEvent

  case object CatalogOperationSuccess extends CatalogEvent

  case object CatalogOperationFailure extends CatalogEvent

}
