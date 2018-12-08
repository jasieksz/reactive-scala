name := "reactive-shop"

version := "0.1"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.17",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.17" % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.typesafe.akka" %% "akka-persistence" % "2.5.18",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "com.typesafe.akka" %% "akka-remote" % "2.5.18",
  "com.typesafe.akka" %% "akka-http" % "10.1.5",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.5",
  "com.typesafe.akka" %% "akka-stream" % "2.5.18",
  "com.typesafe.akka" %% "akka-cluster" % "2.5.18",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.18",
  "io.gatling" % "gatling-http" % "3.0.1")


libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.0.1" % "test,it"
libraryDependencies += "io.gatling" % "gatling-test-framework" % "3.0.1" % "test,it"