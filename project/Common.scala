import sbt._
import Keys._

object Common {
  val appVersion = "0.0.1"
  val settings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.10.4",
    organization := "com.ksquared",
    sbtVersion := "0.13.12"
  )
}