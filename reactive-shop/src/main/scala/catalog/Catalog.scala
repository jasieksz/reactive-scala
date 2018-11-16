package catalog

import java.net.URI

case class Catalog(items: Map[URI, Int]) {

  def addItems(item: URI, quantity: Int): Catalog = {
    val count = if (items contains item) items(item) else 0
    copy(items = items + (item -> (count + quantity)))
  }

  def removeItem(item: URI, quantity: Int): (Catalog, Boolean) = {
    val count = if (items contains item) items(item) else 0
    if (quantity > count) {
      (this, false)
      // TODO : return message
    } else {
      (copy(items = items + (item -> (count - quantity))), true)
    }
  }


  def getCount(item: URI): Int = {
    items.getOrElse(item, 0)
  }

  def getSize: Int = {
    items.values.sum
  }

}


object Catalog {
  val empty = Catalog(Map.empty)
}