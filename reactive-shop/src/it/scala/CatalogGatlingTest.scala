import io.gatling.core.Predef.{ Simulation, rampUsers, scenario, _ }
import io.gatling.http.Predef.http

import scala.concurrent.duration._


class CatalogGatlingTest extends Simulation{

  val httpProtocol = http  //values here are adjusted to cluster_demo.sh script
    .baseUrls("http://localhost:9000")
    .acceptHeader("text/plain,text/html,application/json,application/xml;")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  val scn = scenario("BasicSimulation")
    .exec(
      http("basicSearch")
        .get("/search?keywords=milk,sugar")
        .asJson
    )
    .pause(5)

  setUp(
    scn.inject(rampUsers(500).during(10 seconds))
  ).protocols(httpProtocol)

}
