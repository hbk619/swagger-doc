import sbt._
import Keys._

object SwaggerDocBuild extends sbt.Build {

  import Dependencies._
  val NamePrefix = "swagger-doc"
}