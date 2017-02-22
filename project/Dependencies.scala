import sbt._
import Keys._

object Dependencies {

  val akkaHttpVersion = "10.0.3"
  val akkaDeps: Seq[ModuleID] =  Seq(
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion
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

  val akka = common ++ akkaDeps ++ scala
}