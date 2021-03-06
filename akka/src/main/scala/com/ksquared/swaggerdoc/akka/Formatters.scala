package com.ksquared.swaggerdoc.akka

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.ksquared.swaggerdoc.models._
import spray.json.DefaultJsonProtocol._
import spray.json._

trait Formatters extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val schemaFormatter = jsonFormat1(Schema)
  implicit val propertyFormatter = jsonFormat2(Property)
  implicit val parameterFormatter = jsonFormat4(Parameter)
  implicit val definitionFormatter = jsonFormat2(Definition)
  implicit val responseFormatter = jsonFormat2(Response)
  implicit val tagFormatter = jsonFormat2(Tag)
  implicit val operationFormatter = jsonFormat5(Operation)
  implicit val infoFormatter = jsonFormat2(Info)
  implicit val swaggerFormatter = jsonFormat6(Swagger)
}