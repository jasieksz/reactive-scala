import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import managers.OrderManager
import managers.OrderManager.SearchItem


object ReactiveShop extends App {


  val config = ConfigFactory.load()
  val shopSystem = ActorSystem("shop", config.getConfig("shop").withFallback(config))

  val manager = shopSystem.actorOf(Props(new OrderManager()), "ordermanager")


  Thread.sleep(5000)


  manager ! SearchItem(List("Fanta"))
}

