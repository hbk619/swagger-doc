import sbt._
import Keys._

object Dependencies {

  val akkaHttpVersion = "10.0.3"
  val akkaDeps: Seq[ModuleID] =  Seq(
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
  )

  val scala: Seq[ModuleID] = Seq(
    "org.scala-lang" % "scala-reflect" % "2.11.8"
  )

  val commonsIo: ModuleID =  "commons-io" % "commons-io" % "2.5"
  val sprayJson: ModuleID = "io.spray" %% "spray-json" % "1.3.3"

  val common: Seq[ModuleID] = Seq(
    sprayJson,
    commonsIo
  )

  val testing: Seq[ModuleID] = Seq(
    "org.scalatest" % "scalatest_2.11"  % "3.0.1"   % "test",
    "org.mockito" % "mockito-all" % "1.10.19" % "test"
  )
  val akka = common ++ akkaDeps ++ scala ++ testing
}