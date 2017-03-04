package com.ksquared.swaggerdoc.akka

import java.io.File

import akka.http.scaladsl.model.{HttpRequest, Uri}
import com.ksquared.swaggerdoc.models._
import org.apache.commons.io.FileUtils

import scala.reflect.runtime.universe._
import scala.collection.breakOut
import scala.util.matching.Regex

class Documenter extends Formatters {
  val swaggerDocs = new SwaggerDocs("0.0.1", Some("/"))
  val dir = new File("restdoc/generated")
  val StringType = typeOf[String]
  val StringOptionType = typeOf[Option[String]]
  val IntType = typeOf[Int]
  val IntOptionType = typeOf[Option[Int]]
  val BooleanType = typeOf[Boolean]
  val BooleanOptionType = typeOf[Option[Boolean]]
  val ListType = typeOf[List[_]]
  val ListOptionType = typeOf[Option[List[_]]]

  def documentRequest[T](req: HttpRequest, body: T) = {
    val definition = createDefinition(body)
    val definitionName = body.getClass.getSimpleName
    val url = req.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val parameter = Parameter("body", definitionName, Some(Schema(s"#/definitions/$definitionName")), None)
    val operation = Operation(List("application/json"), List("application/json"), Seq(parameter), Map())
    swaggerDocs.addOperation(url.path.toString(), req.method.value.toLowerCase(), operation)
    swaggerDocs.addDefinition(definitionName, definition)
  }

  def documentRequest(req: HttpRequest, pathRegEx: Regex) = {
    val url = req.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val parameters = getUrlParameters(url, pathRegEx)
    val operation = Operation(List("application/json"), List("application/json"), parameters, Map())
    swaggerDocs.addOperation(url.path.toString(), req.method.value.toLowerCase, operation)
  }

  def saveResponse(name: String) = {
    writeToFile(createFile(name))
  }

  def createFile(name: String) = {
    val baseDir = new File(dir, name)
    baseDir.mkdirs()
    new File(baseDir, "swagger.json")
  }

  def writeToFile(file: File) = {
    FileUtils.write(file, swaggerFormatter.write(swaggerDocs.swagger).toString(), "UTF-8")
  }

  private def createDefinition[T](body: T): Definition = {
    val rm = scala.reflect.runtime.currentMirror
    val accessors = rm.classSymbol(body.getClass).toType.members.collect {
      case m: MethodSymbol if m.isGetter && m.isPublic => m
    }
    val properties: Map[String, Property] = accessors.map(m => {
      (m.name.toString, createProperty(m))
    })(breakOut)

    Definition("object", properties)
  }

  private def createProperty(propertyMethodSymbol: MethodSymbol): Property = {
    propertyMethodSymbol.returnType match {
      case StringType | StringOptionType => Property("string")
      case IntType | IntOptionType => Property("integer")
      case BooleanType | BooleanOptionType => Property("boolean")
      case x if x <:< ListType | x <:< ListOptionType => Property("array")
      case _ => Property("")
    }
  }

  private def getUrlParameters(url: Uri, pathRegEx: Regex) = {
    pathRegEx.findAllMatchIn(url.path.toString())
      .toArray.flatMap { m =>
        m.groupNames.map { name =>
          Parameter("path", name, None, Some("string"))
        }
      }
  }
}
