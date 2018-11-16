package catalog

import java.net.URI

import akka.actor.Actor
import catalog.CatalogSupervisor.{CatalogOperationFailure, LookUpItem, LookUpResult}
import model.Item

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success}


class SearchWorker extends Actor {
  val dbPath: String = "resources/product_db"

  override def receive: Receive = {
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
    val bufferedSource: BufferedSource = Source.fromFile(path)

    val items = bufferedSource.getLines()
      .map(line => line.split(","))
      .filter(line => line(1).concat(line(2)).contains(name))
      .map(line => Item(URI.create(line(0)), line(1), BigDecimal.apply(5)))
      .toList

    bufferedSource.close
    items
  }

}

