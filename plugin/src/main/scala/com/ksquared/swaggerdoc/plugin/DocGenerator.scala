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
  implicit val schemaFormatter = jsonFormat1(Schema)
  implicit val propertyFormatter = jsonFormat1(Property)
  implicit val parameterFormatter = jsonFormat4(Parameter)
  implicit val definitionFormatter = jsonFormat2(Definition)
  implicit val responseFormatter = jsonFormat2(Response)
  implicit val operationFormatter = jsonFormat4(Operation)
  implicit val infoFormatter = jsonFormat2(Info)
  implicit val swaggerFormatter = jsonFormat5(Swagger)

  def generateDoc(docsLocation: File, apiVersion: String, baseUrl: String) = {
    val swaggerDocs = new SwaggerDocs(apiVersion, Some(baseUrl))

    val files: util.Collection[File] = getFiles(docsLocation, "json")

    files.iterator().asScala.toStream.foreach(file => {
      val lines = getFileContents(file)

      val swagger: Swagger = swaggerFormatter.read(lines.parseJson)

      swaggerDocs.addPaths(swagger.paths)
      swaggerDocs.addDefinitions(swagger.definitions)
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
