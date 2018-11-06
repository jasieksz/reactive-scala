import java.util.UUID

import akka.actor.{ActorSystem, Props}
import managers.OrderManager

object ReactiveShop extends App {
  val system = ActorSystem("system")

  val manager = system.actorOf(Props(new OrderManager()))

  manager ! OrderManager.AddItem(UUID.randomUUID())
  manager ! OrderManager.StartCheckout

  Thread.sleep(1000)

  manager ! OrderManager.SelectDeliveryMethod("poczta")
  manager ! OrderManager.SelectPaymentMethod("visa")

//  Thread.sleep(2000)
//
//  manager ! managers.OrderManager.GetParametersForTest


  manager ! OrderManager.Buy

  Thread.sleep(1000)

  manager ! OrderManager.Pay
}
