import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import managers.{OrderManager, PaymentManager}
import managers.PaymentManager.Pay


object ReactiveShop extends App {


  val config = ConfigFactory.load()
  val shopSystem = ActorSystem("shop", config.getConfig("shop").withFallback(config))

  val manager = shopSystem.actorOf(Props(new OrderManager()), "ordermanager")

  val paymentManager = shopSystem.actorOf(Props(new PaymentManager(manager)), "paymentmanager")

  Thread.sleep(5000)
  paymentManager ! Pay(manager)

  Thread.sleep(500)
  paymentManager ! Pay(manager)

  Thread.sleep(500)
  paymentManager ! Pay(manager)

  Thread.sleep(500)
  paymentManager ! Pay(manager)

  Thread.sleep(5000)
  paymentManager ! Pay(manager)

  Thread.sleep(500)
  paymentManager ! Pay(manager)

  Thread.sleep(500)
  paymentManager ! Pay(manager)

  Thread.sleep(500)
  paymentManager ! Pay(manager)

}

