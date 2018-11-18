package catalog

import java.net.URI

import model.Item

import scala.io.{BufferedSource, Source}
import scala.util.Random

class Catalog() {

  private val bufferedSource: BufferedSource = Source.fromResource("product_db")
  private val itemsMap: Map[Item, Int] = bufferedSource
    .getLines()
    .drop(1)
    .map(line => line.replaceAll("\"", ""))
    .map(line => line.split(","))
    .filter(line => line.length == 3)
    .map(line => (Item(URI.create(line(0)), line(1), Random.nextInt(100).toDouble), Random.nextInt(100)))
    .toMap

  def search(keyWords: List[String]): List[Item] = {
    val keys = keyWords.map(k => k.toLowerCase)
    itemsMap.keys.toList
      .map(item => (item, keys.count(item.name.toLowerCase.contains)))
      .sortBy(-_._2)
      .take(10)
      .map(_._1)
  }
}