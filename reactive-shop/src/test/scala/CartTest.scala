import java.net.URI

import model.{Cart, Item}
import org.scalatest._

class CartTest extends FlatSpec {

  val apple: Item = Item(URI.create("apple"), "apple", 1)
  val orange: Item = Item(URI.create("orange"), "orange", 1)
  val plum: Item = Item(URI.create("plum"), "plum", 1)


  "Cart" should "add new items" in {
    // given
    var cart: Cart = Cart.empty

    //when
    cart = cart.addItem(apple)
    cart = cart.addItem(orange)
    cart = cart.addItem(apple)

    //then
    assert(cart.getCount(apple) == 2)
    assert(cart.getCount(orange) == 1)
    assert(cart.getCount(plum) == 0)
  }

  "Cart" should "remove single item" in {
    // given
    var cart: Cart = Cart.empty
    cart = cart.addItem(apple)
    cart = cart.addItem(apple)

    // when
    cart = cart.removeItem(apple)

    //then
    assert(cart.getCount(apple) == 1)
  }

  "Cart" should "remove all items" in {
    // given
    var cart: Cart = Cart.empty
    cart = cart.addItem(apple)
    cart = cart.addItem(apple)

    // when
    cart = cart.removeItems(apple, 5)

    //then
    assert(cart.getCount(apple) == 0)
  }

  "Cart" should "remove many items" in {
    // given
    var cart: Cart = Cart.empty
    cart = cart.addItem(apple)
    cart = cart.addItem(apple)
    cart = cart.addItem(apple)
    cart = cart.addItem(apple)

    // when
    cart = cart.removeItems(apple, 2)

    //then
    assert(cart.getCount(apple) == 2)
  }
}
