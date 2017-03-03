package com.ksquared.swaggerdoc.plugin

import java.io.File
import java.util

import com.ksquared.swaggerdoc.models._
import org.apache.commons.io
import org.apache.commons.io.FileUtils
import sbt.{Def, SettingKey, TaskKey}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.JavaConverters._

object Plugin extends sbt.AutoPlugin {
  implicit val propertyFormatter = jsonFormat1(Property)
  implicit val parameterFormatter = jsonFormat3(Parameter)
  implicit val operationFormatter = jsonFormat2(Operation)
  implicit val modelFormatter = jsonFormat2(Model)
  implicit val apiFormatter = jsonFormat2(Api)
  implicit val swaggerFormatter = jsonFormat6(Swagger)

  val defaultInputLocation: String = System.getProperty("user.dir") + "/restdoc/generated"
  val defaultOutputLocation: String = System.getProperty("user.dir") + "/restdoc/generated"

  override def trigger = allRequirements

  object autoImport {
    lazy val swaggerDocTask = TaskKey[Unit]("swagger-doc", "Creates swagger docs json")
    lazy val swaggerDocConfig = SettingKey[Map[String, String]]("swagger-doc-config", "Base config for swagger doc")
  }

  import autoImport._

  def swaggerDocGenerator = Def.task {
    val config: Map[String, String] = swaggerDocConfig.value
    val location = config.getOrElse("output", Plugin.defaultOutputLocation)
    val outputFile = new File(location)

    val baseUrl = config.getOrElse("baseUrl", "/")
    val apiVersion = config.getOrElse("apiVersion", "0.0.0")
    val swaggerDocs = new SwaggerDocs(apiVersion, Some(baseUrl), Some(List("application/json")))
    val inputFile = new File(config.getOrElse("input", Plugin.defaultInputLocation))

    val files: util.Collection[File] = io.FileUtils.listFiles(inputFile, List("json").toArray, true)

    files.iterator().asScala.toStream.foreach(file => {
      val lines = FileUtils.readFileToString(file, "UTF-8")

      val swagger: Swagger = swaggerFormatter.read(lines.parseJson)

      swagger.apis.foreach(swaggerDocs.addApi)
      swagger.models.foreach(m => {
        swaggerDocs.addModel(m._2)
      })
    })

    val generatedDoc: String = swaggerFormatter.write(swaggerDocs.swagger).toString()
    FileUtils.writeStringToFile(outputFile, generatedDoc, "UTF-8", false)
    generatedDoc
  }

  override lazy val projectSettings = Seq(
    swaggerDocConfig := Map(
      "title" -> "Documentation for an api",
      "baseUrl" -> "/",
      "apiVersion" -> "0.0.1",
      "output" -> defaultOutputLocation,
      "input" -> defaultInputLocation
    ),
    swaggerDocTask := swaggerDocGenerator.value
  )
}