package com.ksquared.swaggerdoc.plugin

import java.io.File
import java.util

import com.ksquared.swaggerdoc.models._
import org.apache.commons.io
import org.apache.commons.io.FileUtils
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.collection.JavaConverters._

trait DocGenerator {
  implicit val propertyFormatter = jsonFormat1(Property)
  implicit val parameterFormatter = jsonFormat3(Parameter)
  implicit val operationFormatter = jsonFormat2(Operation)
  implicit val modelFormatter = jsonFormat2(Model)
  implicit val apiFormatter = jsonFormat2(Api)
  implicit val swaggerFormatter = jsonFormat6(Swagger)

  def generateDoc(docsLocation: File, apiVersion: String, baseUrl: String) = {
    val swaggerDocs = new SwaggerDocs(apiVersion, Some(baseUrl), Some(List("application/json")))

    val files: util.Collection[File] = getFiles(docsLocation, "json")

    files.iterator().asScala.toStream.foreach(file => {
      val lines = getFileContents(file)

      val swagger: Swagger = swaggerFormatter.read(lines.parseJson)

      swagger.apis.foreach(swaggerDocs.addApi)
      swagger.models.foreach(m => {
        swaggerDocs.addModel(m._2)
      })
    })

    swaggerFormatter.write(swaggerDocs.swagger).toString()
  }

  def getFiles(folder: File, extension: String) = {
    io.FileUtils.listFiles(folder, List(extension).toArray, true)
  }

  def getFileContents(file: File) = {
    FileUtils.readFileToString(file, "UTF-8")
  }
}
