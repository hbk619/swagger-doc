package com.ksquared.swaggerdoc.plugin

import java.io.File

import org.apache.commons.io.FileUtils
import spray.json.DefaultJsonProtocol._
import spray.json._
import com.ksquared.swaggerdoc.models._
import sbt.TaskKey

import scala.collection.JavaConverters._

object Plugin extends sbt.AutoPlugin {

  def swaggerDocGenerator() = {
    implicit val propertyFormatter = jsonFormat1(Property)
    implicit val parameterFormatter = jsonFormat3(Parameter)
    implicit val operationFormatter = jsonFormat2(Operation)
    implicit val modelFormatter = jsonFormat2(Model)
    implicit val apiFormatter = jsonFormat2(Api)
    implicit val swaggerFormatter = jsonFormat6(Swagger)
    val dir = new File(System.getProperty("user.dir") + "/restdoc/generated")

    var swaggerDocs = new SwaggerDocs("0.0.1", Some("/"), Some(List("application/json")))

    FileUtils.listFiles(new File(System.getProperty("user.dir") + "/restdoc/generated"), List("json").toArray, true)
      .iterator().asScala.toStream.foreach(file => {
      val lines = FileUtils.readFileToString(file, "UTF-8")

      val swagger: Swagger = swaggerFormatter.read(lines.parseJson)

      swagger.apis.foreach(swaggerDocs.addApi(_))
      swagger.models.foreach(m => {
        swaggerDocs.addModel(m._2)
      })
    })

    val responseFile = new File(dir, "swagger.json")
    FileUtils.writeStringToFile(responseFile, swaggerFormatter.write(swaggerDocs.swagger).toString(), "UTF-8", false)
  }

  override def trigger = allRequirements
  object autoImport {
    lazy val swaggerDocTask = TaskKey[Unit]("swagger-doc", "Creates swagger docs json")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    swaggerDocTask := {
      swaggerDocGenerator
    }
  )

}
