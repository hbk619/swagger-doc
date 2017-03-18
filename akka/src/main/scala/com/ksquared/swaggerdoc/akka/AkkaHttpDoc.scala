package com.ksquared.swaggerdoc.akka

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.RouteTest
import spray.json.RootJsonFormat

import scala.concurrent.duration._
import scala.util.matching.Regex
import spray.json._

import scala.concurrent.Await

case class ResponseDetails(name: String, request: HttpRequest, response: HttpResponse,
                           formattedUrl: String, body: Option[_] = None)

trait AkkaHttpDoc extends Formatters {
  self: RouteTest =>

  def route: Route

  def setup(req: HttpRequest, pathRegEx: Regex) = {
    val swaggerDocs = new Documenter()
    val url = swaggerDocs.documentRequest(req, pathRegEx)
    Request(route, req, swaggerDocs, url)
  }

  def setup[T](req: HttpRequest, body: T) = {
    val documenter = new Documenter()
    val url = documenter.documentRequest(req, body)
    Request(route, req, documenter, url)
  }

  def setup[T](req: HttpRequest, body: T, pathRegEx: Regex) = {
    val documenter = new Documenter()
    val url = documenter.documentRequest(req, body, pathRegEx)
    Request(route, req, documenter, url)
  }

  case class Request(route: Route, req: HttpRequest, documenter: Documenter, formattedUrl: String) {

    def perform[T, U](name: String)(body: => T)(implicit formatter: RootJsonFormat[U]): T = {
      req ~> Route.seal(route) ~> check {
        val getEntity = response.entity.toStrict(5.seconds)
          .map(_.data.decodeString("UTF-8"))
        val responseString = Await.result(getEntity,10.seconds)
        documenter.saveResponse(ResponseDetails(name, req, response, formattedUrl, Some(formatter.read(responseString.parseJson))))
        body
      }
    }
  }
}