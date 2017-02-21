package com.swaggerdoc

import java.io.File

import sbt.{Command, File, TaskKey}
import spray.json._
import DefaultJsonProtocol._
import org.apache.commons.io.FileUtils

import scala.collection.JavaConverters._

case class Property(`type`: String)

case class Parameter(`type`: String)

case class Model(name: String, properties: Map[String, Property])

case class Operation(method: String, parameters: Seq[Parameter])

case class Api(path: String, operations: Seq[Operation])

case class Swagger(api: List[Api], models: Map[String, Model])

class SwaggerDocs {
  var swagger = Swagger(List(), Map())

  def addApi(api: Api) = {
    if (!swagger.api.contains(api)) {
      val apis = api :: swagger.api
      swagger = swagger.copy(api = apis)
    }
  }

  def addModel(model: Model): Unit = {
    if (swagger.models.get(model.name).isEmpty) {
      val models = swagger.models + (model.name -> model)
      swagger = swagger.copy(models = models)
    }
  }
}

object Plugin extends sbt.AutoPlugin {
  def swaggerDocGenerator() = {
    implicit val propertyFormatter = jsonFormat1(Property)
    implicit val parameterFormatter = jsonFormat1(Parameter)
    implicit val operationFormatter = jsonFormat2(Operation)
    implicit val modelFormatter = jsonFormat2(Model)
    implicit val apiFormatter = jsonFormat2(Api)
    implicit val swaggerFormatter = jsonFormat2(Swagger)
    val dir = new File(System.getProperty("user.dir") + "/restdoc/generated")

    var swaggerDocs = new SwaggerDocs()

    FileUtils.listFiles(new File(System.getProperty("user.dir") + "/restdoc/generated"), List("json").toArray, true)
      .iterator().asScala.toStream.foreach(file => {
      val lines = FileUtils.readFileToString(file, "UTF-8")

      val swagger: Swagger = swaggerFormatter.read(lines.parseJson)

      swagger.api.foreach(swaggerDocs.addApi(_))
      swagger.models.foreach(m => {
        swaggerDocs.addModel(m._2)
      })
    })

    val responseFile = new File(dir, "swagger.json")
    FileUtils.writeStringToFile(responseFile, swaggerFormatter.write(swaggerDocs.swagger).toString(), "UTF-8", false)
  }

  override def trigger = allRequirements
  object autoImport {
    lazy val swaggerDoc = TaskKey[Unit]("swagger-doc", "Creates swagger docs json")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    swaggerDoc := {
      swaggerDocGenerator
    }
  )

}
