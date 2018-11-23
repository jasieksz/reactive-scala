import catalog.Catalog
import org.scalatest.{FlatSpec, Matchers}

class CatalogTest extends FlatSpec with Matchers {

  "A Catalog" should "load the file from resources" in {
    val catalog = new Catalog()
    catalog.size shouldBe 682263
  }

}
