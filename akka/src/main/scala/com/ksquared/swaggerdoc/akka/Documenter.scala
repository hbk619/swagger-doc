package com.ksquared.swaggerdoc.akka

import java.io.File

import akka.http.scaladsl.model.{HttpRequest, Uri}
import com.ksquared.swaggerdoc.models._
import org.apache.commons.io.FileUtils

import scala.reflect.runtime.universe._
import scala.collection.breakOut

class Documenter extends Formatters {
  val swaggerDocs = new SwaggerDocs()
  val dir = new File("restdoc/generated")

  def documentRequest[T](req: HttpRequest, body: T) = {
    val model = createModel(body)
    val url = req.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val parameter = Parameter(model.name)
    val operation = Operation(req.method.value, Seq(parameter))
    val api = Api(url.path.toString(), Seq(operation))
    swaggerDocs.addApi(api)
    swaggerDocs.addModel(model)
  }

  def documentRequest(req: HttpRequest) = {
    val url = req.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val operation = Operation(req.method.value, Seq())
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
      (m.name.toString, Property("string"))
    })(breakOut)

    Model(body.getClass.getSimpleName, properties)
  }
}
