import java.net.URI

import akka.actor.{ActorSystem, Props}
import managers.CartManager.{AddItem, CartStarted, RemoveItem, StartCart}
import managers.OrderManager.GetCart
import managers.{CartManager, OrderManager}
import model.Item

object ReactiveShop extends App {
  val system = ActorSystem("system")

  val manager = system.actorOf(Props(new OrderManager()))

  val apple: Item = Item(URI.create("apple"), "apple", 1)
  val orange: Item = Item(URI.create("orange"), "orange", 1)

  Thread.sleep(1000)

  manager ! OrderManager.AddItem(apple)
  manager ! OrderManager.AddItem(orange)

  manager ! OrderManager.StartCheckout

  Thread.sleep(1000)

  manager ! OrderManager.SelectDeliveryMethod("poczta")
  manager ! OrderManager.SelectPaymentMethod("visa")

  Thread.sleep(1000)

  manager ! OrderManager.Buy

  Thread.sleep(1000)

  manager ! OrderManager.Pay

  Thread.sleep(3000)

  system.terminate()
}
