package com.ksquared.swaggerdoc.akka

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.ksquared.swaggerdoc.models._
import spray.json.DefaultJsonProtocol._
import spray.json._

trait Formatters extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val propertyFormatter = jsonFormat1(Property)
  implicit val parameterFormatter = jsonFormat1(Parameter)
  implicit val operationFormatter = jsonFormat2(Operation)
  implicit val modelFormatter = jsonFormat2(Model)
  implicit val apiFormatter = jsonFormat2(Api)
  implicit val swaggerFormatter = jsonFormat2(Swagger)
}