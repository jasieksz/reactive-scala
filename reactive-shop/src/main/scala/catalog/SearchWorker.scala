package catalog

import akka.actor.Actor
import akka.event.LoggingReceive
import catalog.CatalogSupervisor.{SearchItem, SearchResult}

class SearchWorker(catalog: Catalog) extends Actor {

  override def receive: Receive = LoggingReceive {
    case SearchItem(keyWords, replyTo) =>
      val response = catalog.search(keyWords)
      sender() ! SearchResult(response) // TODO : Change to replyTo
  }
}

