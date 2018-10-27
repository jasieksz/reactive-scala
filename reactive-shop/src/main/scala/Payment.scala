import java.util.UUID

import akka.actor.Timers
import akka.event.LoggingReceive

import scala.concurrent.duration._


class Payment extends Timers {

  override def receive: Receive = initial()

  def initial(): LoggingReceive = {
    case _ => _
  }
}

object Payment {

  sealed trait Data

  case class PaymentConfirmed(id: UUID) extends Data
  case class PaymentReceived(id: UUID) extends Data

}