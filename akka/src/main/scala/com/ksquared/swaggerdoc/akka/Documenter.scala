package com.ksquared.swaggerdoc.akka

import java.io.File

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
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
    val definitions = createDefinition(body)
    val definitionName = body.getClass.getSimpleName
    val url = req.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val parameter = Parameter("body", definitionName, Some(Schema(s"#/definitions/$definitionName")), None)
    val operation = Operation(Set("application/json"), Set("application/json"), Set(parameter), Map())
    swaggerDocs.addOperation(url.path.toString(), req.method.value.toLowerCase(), operation)
    swaggerDocs.addDefinitions(definitions)
  }

  def documentRequest(req: HttpRequest, pathRegEx: Regex) = {
    val url = req.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val parameters = getUrlParameters(url, pathRegEx)
    val operation = Operation(Set("application/json"), Set("application/json"), parameters, Map())
    swaggerDocs.addOperation(url.path.toString(), req.method.value.toLowerCase, operation)
  }

  def saveResponse[T](name: String, request: HttpRequest, response: HttpResponse, responseBody: Option[T] = None) = {
    val url = request.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val method = request.method.value.toLowerCase
    val path = url.path.toString()
    var schema: Option[Schema] = None
    if (responseBody.isDefined) {
        val definitions = createDefinition(responseBody.get)
        val definitionName = responseBody.get.getClass.getSimpleName
        swaggerDocs.addDefinitions(definitions)
        schema = Some(Schema(s"#/definitions/$definitionName"))
    }

    val responseObj = Response(response.status.reason(), schema)

    swaggerDocs.addResponse(path, method, response.status.intValue(), responseObj)
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

  private def createDefinition[T](body: T): Map[String, Definition] = {
    val rm = scala.reflect.runtime.currentMirror
    createDefinitionsFromSymbol(rm.classSymbol(body.getClass))
  }

  private def createDefinitionsFromSymbol(body: scala.reflect.runtime.universe.ClassSymbol): Map[String, Definition] = {
    val definitionName = body.name.toString
    val accessors = body.toType.members.collect {
      case m: MethodSymbol if m.isGetter && m.isPublic => m
    }

    val properties: Map[String, Property] = accessors.map(m => {
      val property: Property = createProperty(m)
      (m.name.toString, property)
    })(breakOut)

    val definitions = properties
      .filter(p => {
        p._2.$ref.isDefined
      })
      .flatMap(p => {
        val methodSymbol: MethodSymbol = accessors.find( m => {
          m.name.toString.equals(p._1)
        }).get
        createDefinitionsFromSymbol(methodSymbol.returnType.typeSymbol.asClass)
      })

    definitions + (definitionName -> Definition("object", properties))
  }

  private def createProperty(propertyMethodSymbol: MethodSymbol): Property = {
    propertyMethodSymbol.returnType match {
      case StringType | StringOptionType => Property(Some("string"))
      case x if x <:< IntType | x <:< IntOptionType => Property(Some("integer"))
      case x if x <:< BooleanType | x <:< BooleanOptionType => Property(Some("boolean"))
      case x if x <:< ListType | x <:< ListOptionType => Property(Some("array"))
      case returnType @ _ => Property(None, Some(s"#/definitions/${returnType.typeSymbol.name.toString}"))
    }
  }

  private def getUrlParameters(url: Uri, pathRegEx: Regex): Set[Parameter] = {
    pathRegEx.findAllMatchIn(url.path.toString())
      .toSet.flatMap { m: Regex.Match =>
        m.groupNames.map { name =>
          Parameter("path", name, None, Some("string"))
        }
      }
  }
}
