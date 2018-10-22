import Cart.ItemAdded
import akka.actor.{ActorSystem, Props}
import Checkout._

object ReactiveShop extends App{
  val system = ActorSystem("system")

  val cart = system.actorOf(Props(new Cart()))
  val checkout = system.actorOf(Props(new Checkout()))

  cart ! ItemAdded
  cart ! ItemAdded

  checkout ! DeliveryMethodSelected("inpost")
  checkout ! PaymentSelected("visa")
  checkout ! PaymentReceived
  checkout ! Cancelled
}
