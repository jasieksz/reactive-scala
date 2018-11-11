package model

case class Cart(items: Map[Item, Int]) {

  def addItem(item: Item): Cart = {
    val count = if (items contains item) items(item) else 0
    copy(items = items + (item -> (count + 1)))
  }

  def removeItem(item: Item): Cart = {
    val count = if (items contains item) items(item) else 0
    if (count > 1) {
      copy(items = items + (item -> (count - 1)))
    } else {
      copy(items = items - item)
    }
  }

  def removeItems(item: Item, count: Int): Cart = {
    if (count == 0) {
      return this
    }
    removeItem(item).removeItems(item, count - 1)
  }

  def getCount(item: Item): Int = {
    items.getOrElse(item, 0)
  }

  def getSize(): Int = {
    items.values.reduceLeft(_ + _)
  }

}

object Cart {
  val empty = Cart(Map.empty)
}