package com.swaggerdoc.akka

import org.apache.commons.io.FileUtils
import spray.json._
import DefaultJsonProtocol._

import scala.collection.breakOut
import scala.reflect.runtime.universe._
import scala.concurrent.Await
import java.io.File

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.RouteTest
import com.swaggerdoc.models._

trait AkkaHttpRestDoc {
  self: RouteTest =>

  def route: Route

  import scala.concurrent.duration._

  val dir = new File("restdoc/generated")

  def perform(req: HttpRequest) = {
    val swaggerDocs = new SwaggerDocs()
    val operation = Operation(req.method.value, Seq())
    val url = req.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val api = Api(url.path.toString(), Seq(operation))
    swaggerDocs.addApi(api)
    RequestAvailable(route, req, swaggerDocs)
  }

  def performWithBody[T](req: HttpRequest, body: T) = {
    val swaggerDocs = new SwaggerDocs()
    val model = createModel(body)
    val url = req.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val parameter = Parameter(model.name)
    val operation = Operation(req.method.value, Seq(parameter))
    val api = Api(url.path.toString(), Seq(operation))
    val models = Map(model.name -> model)
    swaggerDocs.addApi(api)
    swaggerDocs.addModel(model)
    RequestAvailable(route, req, swaggerDocs)
  }

  case class RequestAvailable(route: Route, req: HttpRequest, swaggerDocs: SwaggerDocs) {
    def checkAndDocument[T](name: String)(body: => T): T = {
      req ~> Route.seal(route) ~> check {
        val baseDir = new File(dir, name)
        baseDir.mkdirs()
        val responseFile = new File(baseDir, "swagger.json")
        val result = body
        FileUtils.write(responseFile, swaggerFormatter.write(swaggerDocs.swagger).toString())
        result
      }
    }
  }

  implicit val propertyFormatter = jsonFormat1(Property)
  implicit val parameterFormatter = jsonFormat1(Parameter)
  implicit val operationFormatter = jsonFormat2(Operation)
  implicit val modelFormatter = jsonFormat2(Model)
  implicit val apiFormatter = jsonFormat2(Api)
  implicit val swaggerFormatter = jsonFormat2(Swagger)

  private def createModel[T](body: T): Model = {
    val rm = scala.reflect.runtime.currentMirror
    val accessors = rm.classSymbol(body.getClass).toType.members.collect {
      case m: MethodSymbol if m.isGetter && m.isPublic => m
    }
    val properties: Map[String, Property] = accessors.map(m => {
      (m.name.toString, Property("String"))
    })(breakOut)

    Model(body.getClass.getSimpleName, properties)
  }

  private def formatRequest(req: HttpRequest): String = {
    val url = req.uri.toEffectiveHttpRequestUri(Uri.Host("localhost"), 8080)
    val headers = req.headers.map(formatHeaderCurl).mkString(" ")
    val body = formatBody(req.entity)

    s"""[source,bash]
       |----
       |curl $url -i $headers $body
       |----
    """.stripMargin
  }

  private def formatHeaderCurl(header: HttpHeader): String = {
    val name = header.name()
    val value = header.value()
    s"-H '$name: $value'"
  }

  private def formatBody(entity: RequestEntity): String = {
    val bodyString = Await.result(entity.toStrict(1.second).map { s => s.data.decodeString("utf-8") }, 1.second)
    if (bodyString.nonEmpty)
      s"-d '${bodyString.parseJson.prettyPrint}'"
    else
      bodyString
  }

  private def formatResponse(response: HttpResponse): String = {
    val headers = response.headers.map(formatHeaderResponse).mkString("\n")
    val body = formatEntity(response.entity)
    s"""----
       |$headers
       |
       |$body
       |----
     """.stripMargin
  }

  private def formatHeaderResponse(header: HttpHeader): String = {
    val name = header.name()
    val value = header.value()
    s"$name: $value"
  }

  private def formatContentType(ct: ContentType): String = {
    val ctString = ct.toString()
    s"Content-Type: $ctString"
  }

  private def formatEntity(entity: HttpEntity): String = {
    Await.result(entity.toStrict(1.second).map { strict =>
      strict.contentType.mediaType.value match {
        case "text/plain" =>
          strict.data.decodeString("utf-8")
        case "application/json" =>
          strict.data.decodeString("utf-8").parseJson.prettyPrint
        case _ =>
          "[BINARY DATA]"
      }
    }, 1.second)
  }
}
