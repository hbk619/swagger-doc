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
  val AnyOption = typeOf[Option[_]]

  def documentRequest[T](req: HttpRequest, body: T): String = {
    val definitions = createDefinition(body)
    val definitionName = body.getClass.getSimpleName
    val url = req.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val path = url.path.toString()
    val parameter = Parameter("body", definitionName, Some(Schema(s"#/definitions/$definitionName")), None)
    val operation = Operation(Set("application/json"), Set("application/json"), Set(parameter), Map())
    swaggerDocs.addOperation(path, req.method.value.toLowerCase(), operation)
    swaggerDocs.addDefinitions(definitions)
    path
  }

  def documentRequest[T](req: HttpRequest, body: T, pathRegEx: Regex): String = {
    val definitions = createDefinition(body)
    val definitionName = body.getClass.getSimpleName
    val url = req.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val parameter = Parameter("body", definitionName, Some(Schema(s"#/definitions/$definitionName")), None)
    val parameters = getUrlParameters(url, pathRegEx)
    val operation = Operation(Set("application/json"), Set("application/json"), parameters + parameter, Map())
    val parameterisedUrl = formatUrl(url, pathRegEx)
    swaggerDocs.addOperation(parameterisedUrl, req.method.value.toLowerCase(), operation)
    swaggerDocs.addDefinitions(definitions)
    parameterisedUrl
  }

  def documentRequest(req: HttpRequest, pathRegEx: Regex): String = {
    val url = req.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val parameters = getUrlParameters(url, pathRegEx)
    val operation = Operation(Set("application/json"), Set("application/json"), parameters, Map())
    val parameterisedUrl = formatUrl(url, pathRegEx)
    swaggerDocs.addOperation(parameterisedUrl, req.method.value.toLowerCase, operation)
    parameterisedUrl
  }

  def saveResponse[T](responseDetails: ResponseDetails) = {
    val method = responseDetails.request.method.value.toLowerCase
    var schema: Option[Schema] = None
    if (responseDetails.body.isDefined) {
      val definitions = createDefinition(responseDetails.body.get)
      val definitionName = responseDetails.body.get.getClass.getSimpleName
      swaggerDocs.addDefinitions(definitions)
      schema = Some(Schema(s"#/definitions/$definitionName"))
    }

    val responseObj = Response(responseDetails.response.status.reason(), schema)

    swaggerDocs.addResponse(responseDetails.formattedUrl, method, responseDetails.response.status.intValue(), responseObj)
    writeToFile(createFile(responseDetails.name))
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
        val methodSymbol: MethodSymbol = accessors.find(m => {
          m.name.toString.equals(p._1)
        }).get
        methodSymbol.returnType match {
          case x if x <:< AnyOption =>
            createDefinitionsFromSymbol(methodSymbol.returnType.typeArgs.head.typeSymbol.asClass)
          case _ =>
            createDefinitionsFromSymbol(methodSymbol.returnType.typeSymbol.asClass)
        }

      })

    definitions + (definitionName -> Definition("object", properties))
  }

  private def createProperty(propertyMethodSymbol: MethodSymbol): Property = {
    propertyMethodSymbol.returnType match {
      case StringType | StringOptionType => Property(Some("string"))
      case x if x <:< IntType | x <:< IntOptionType => Property(Some("integer"))
      case x if x <:< BooleanType | x <:< BooleanOptionType => Property(Some("boolean"))
      case x if x <:< ListType | x <:< ListOptionType => Property(Some("array"))
      case x if x <:< AnyOption => Property(None, Some(s"#/definitions/${x.typeArgs.head.typeSymbol.name.toString}"))
      case returnType @ _ => Property(None, Some(s"#/definitions/${returnType.typeSymbol.name.toString}"))
    }
  }

  private def formatUrl(url: Uri, pathRegEx: Regex): String = {
    var path = url.path.toString()
    pathRegEx.findAllMatchIn(path)
      .toSet.foreach { m: Regex.Match =>
      m.groupNames.foreach { name =>
        path = path.replace(m.group(name), s"{$name}")
      }
    }
    path
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
