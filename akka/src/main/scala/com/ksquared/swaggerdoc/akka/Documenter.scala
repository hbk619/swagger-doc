package com.ksquared.swaggerdoc.akka

import java.io.File

import akka.http.scaladsl.model.{HttpRequest, Uri}
import com.ksquared.swaggerdoc.models._
import org.apache.commons.io.FileUtils

import scala.reflect.runtime.universe._
import scala.collection.breakOut
import scala.util.matching.Regex

class Documenter extends Formatters {
  val swaggerDocs = new SwaggerDocs("0.0.1", Some("/"), Some(List("application/json")))
  val dir = new File("restdoc/generated")
  val StringType = typeOf[String]
  val IntType = typeOf[Int]
  val BooleanType = typeOf[Boolean]
  val ListType = typeOf[List[_]]

  def documentRequest[T](req: HttpRequest, body: T) = {
    val model = createModel(body)
    val url = req.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val parameter = Parameter(model.name, "body", "body")
    val operation = Operation(req.method.value, Seq(parameter))
    val api = Api(url.path.toString(), Seq(operation))
    swaggerDocs.addApi(api)
    swaggerDocs.addModel(model)
  }

  def documentRequest(req: HttpRequest, pathRegEx: Regex) = {
    val url = req.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val parameters = getUrlParameters(url, pathRegEx)
    val operation = Operation(req.method.value, parameters)
    val api = Api(url.path.toString(), Seq(operation))
    swaggerDocs.addApi(api)
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

  private def createModel[T](body: T): Model = {
    val rm = scala.reflect.runtime.currentMirror
    val accessors = rm.classSymbol(body.getClass).toType.members.collect {
      case m: MethodSymbol if m.isGetter && m.isPublic => m
    }
    val properties: Map[String, Property] = accessors.map(m => {
      (m.name.toString, createProperty(m))
    })(breakOut)

    Model(body.getClass.getSimpleName, properties)
  }

  private def createProperty(propertyMethodSymbol: MethodSymbol): Property = {
    propertyMethodSymbol.returnType match {
      case StringType => Property("string")
      case IntType => Property("integer")
      case BooleanType => Property("boolean")
      case x if x <:< ListType => Property("array")
      case _ => Property("")
    }
  }

  private def getUrlParameters(url: Uri, pathRegEx: Regex) = {
    pathRegEx.findAllMatchIn(url.path.toString())
      .toArray.flatMap { m =>
        m.groupNames.map { name =>
          Parameter("string", "path", name)
        }
      }
  }
}
