package com.ksquared.swaggerdoc.akka

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.RouteTest

import scala.util.matching.Regex

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

  case class Request(route: Route, req: HttpRequest, documenter: Documenter) extends Formatters {

    def perform[T](name: String)(body: => T): T = {
      req ~> Route.seal(route) ~> check {
        documenter.saveResponse(name)
        body
      }
    }
  }
}