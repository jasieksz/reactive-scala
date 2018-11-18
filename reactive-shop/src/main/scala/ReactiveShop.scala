import java.net.URI

import akka.actor.{ActorSystem, Props}
import catalog.CatalogSupervisor.LookUpItem
import com.typesafe.config.ConfigFactory
import managers.OrderManager
import managers.OrderManager.AddItem
import model.Item

object ReactiveShop extends App {
  val config = ConfigFactory.load("shop_application.conf")
  val system = ActorSystem("shop", config)

  val manager = system.actorOf(Props(new OrderManager()))

  val apple: Item = Item(URI.create("apple"), "apple", 1)
  val orange: Item = Item(URI.create("orange"), "orange", 1)


  Thread.sleep(3000)

//  manager ! OrderManager.AddItem(apple)
//  manager ! OrderManager.AddItem(orange)
//
//  manager ! OrderManager.StartCheckout
//
//  Thread.sleep(1000)
//
//  manager ! OrderManager.SelectDeliveryMethod("poczta")
//  manager ! OrderManager.SelectPaymentMethod("visa")
//
//  Thread.sleep(1000)
//
//  manager ! OrderManager.Buy
//
//  Thread.sleep(1000)
//
//  manager ! OrderManager.Pay
//
//  Thread.sleep(3000)

  manager ! LookUpItem("Fanta", manager)

  manager ! AddItem(apple)
}

/*
"ean","name","brand"
"0000040822938","Fanta orange","Fanta"
 */