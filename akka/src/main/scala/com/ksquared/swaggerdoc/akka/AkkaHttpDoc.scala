package com.ksquared.swaggerdoc.akka

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.RouteTest
import spray.json.RootJsonFormat

import scala.concurrent.duration._
import scala.util.matching.Regex
import spray.json._

import scala.concurrent.Await

trait AkkaHttpDoc extends Formatters {
  self: RouteTest =>

  def route: Route

  def setup(req: HttpRequest, pathRegEx: Regex) = {
    val swaggerDocs = new Documenter()
    swaggerDocs.documentRequest(req, pathRegEx)
    Request(route, req, swaggerDocs)
  }

  def setup[T](req: HttpRequest, body: T) = {
    val documenter = new Documenter()
    documenter.documentRequest(req, body)
    Request(route, req, documenter)
  }

  case class Request(route: Route, req: HttpRequest, documenter: Documenter) {

    def perform[T, U](name: String)(body: => T)(implicit formatter: RootJsonFormat[U]): T = {
      req ~> Route.seal(route) ~> check {
        val getEntity = response.entity.toStrict(5.seconds)
          .map(_.data.decodeString("UTF-8"))
        val responseString = Await.result(getEntity,10.seconds)
        documenter.saveResponse(name, req, response, Some(formatter.read(responseString.parseJson)))
        body
      }
    }
  }
}