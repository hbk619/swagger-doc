package com.ksquared.swaggerdoc.akka

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.RouteTest

trait AkkaHttpDoc extends Formatters {
  self: RouteTest =>

  def route: Route

  def setup(req: HttpRequest) = {
    val swaggerDocs = new Documenter()
    swaggerDocs.documentRequest(req)
    Request(route, req, swaggerDocs)
  }

  def setupRequestWithBody[T](req: HttpRequest, body: T) = {
    val documenter = new Documenter()
    documenter.documentRequest(req, body)
    Request(route, req, documenter)
  }

  case class Request(route: Route, req: HttpRequest, documenter: Documenter) extends Formatters {

    def perform[T](name: String)(body: => T): T = {
      req ~> Route.seal(route) ~> check {
        documenter.saveResponse(name)
        body
      }
    }
  }
}