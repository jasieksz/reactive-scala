package catalog

import java.net.URI

import akka.actor.Actor
import akka.event.LoggingReceive
import catalog.CatalogSupervisor.{CatalogOperationFailure, LookUpItem, LookUpResult}
import model.Item

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success}


class SearchWorker extends Actor {
  val dbPath: String = "resources/product_db"

  override def receive: Receive = LoggingReceive {
    case LookUpItem(name, replyTo) =>
      searchDatabase(dbPath, name) onComplete {
        case Success(result) =>
          replyTo ! LookUpResult(result)
        case Failure(exception) => {
          replyTo ! CatalogOperationFailure
          exception.printStackTrace()
          // TODO : pattern match exception types
        }
      }
  }


  def searchDatabase(path: String, name: String): Future[List[Item]] = Future {
    val bufferedSource: BufferedSource = Source.fromResource("product_db")
    val items = bufferedSource.getLines().drop(1).map(line => line.replaceAll("\"", ""))
      .map(line => line.split(","))
      .filter(line => line.length == 3)
      .filter(line => line(1).contains(name))
      .map(line => Item(URI.create(line(0)), line(1), BigDecimal.apply(5)))
      .toList

    bufferedSource.close
    items
  }

}

