package com.ksquared.swaggerdoc.akka

import java.io.File

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpEntity, HttpProtocols, HttpResponse, StatusCode}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.ksquared.swaggerdoc.models._
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import spray.json.DefaultJsonProtocol._
import spray.json._

class DocumenterSpec extends WordSpec with Matchers
  with ScalatestRouteTest with SprayJsonSupport
  with DefaultJsonProtocol with MockitoSugar {

  case class TestClass(id: String, value: Int, isTrue: Boolean, someList: List[String])
  case class TestOptionalClass(id: Option[String], value: Option[Int], isTrue: Option[Boolean], someList: Option[List[String]])
  implicit val testClassFormatter: RootJsonFormat[TestClass] = jsonFormat4(TestClass)
  implicit val testOptionalClassFormatter: RootJsonFormat[TestOptionalClass] = jsonFormat4(TestOptionalClass)

  "Documenter" should {

    "record request" in {
      val documenter = new Documenter()
      val body = TestClass("123", 21, isTrue = false, List("bob"))
      val req = Post("/test", body)

      documenter.documentRequest(req, body)

      documenter.swaggerDocs.swagger.definitions.keys.size shouldEqual 1
      documenter.swaggerDocs.swagger.paths.size shouldEqual 1
      val model: Definition = documenter.swaggerDocs.swagger.definitions("TestClass")
      model.properties.keys.size shouldEqual 4
      model.properties("id").`type` shouldBe "string"
      model.properties("value").`type` shouldBe "integer"
      model.properties("isTrue").`type` shouldBe "boolean"
      model.properties("someList").`type` shouldBe "array"

      val operations: Map[String, Operation] = documenter.swaggerDocs.swagger.paths("/test")
      operations.size shouldEqual 1
      operations.get("post").isDefined shouldEqual true
      operations("post").parameters.size shouldEqual 1
      operations("post").parameters.head.`in` shouldEqual "body"
      operations("post").parameters.head.schema.get.$ref shouldEqual "#/definitions/TestClass"
    }

    "record optional types request" in {
      val documenter = new Documenter()
      val body = TestOptionalClass(Some("123"), Some(21), Some(false), Some(List("bob")))
      val req = Post("/test", body)

      documenter.documentRequest(req, body)

      documenter.swaggerDocs.swagger.definitions.keys.size shouldEqual 1
      documenter.swaggerDocs.swagger.paths.keys.size shouldEqual 1
      val model: Option[Definition] = documenter.swaggerDocs.swagger.definitions.get("TestOptionalClass")
      model.isDefined shouldBe true
      model.get.properties.keys.size shouldEqual 4
      model.get.properties("id").`type` shouldBe "string"
      model.get.properties("value").`type` shouldBe "integer"
      model.get.properties("isTrue").`type` shouldBe "boolean"
      model.get.properties("someList").`type` shouldBe "array"

      val operations: Map[String, Operation] = documenter.swaggerDocs.swagger.paths("/test")
      operations.get("post").isDefined shouldEqual true
      operations("post").parameters.size shouldEqual 1
      operations("post").parameters.head.schema.get.$ref shouldEqual "#/definitions/TestOptionalClass"
    }

    "record path regex request" in {
      val documenter = new Documenter()
      val req = Get("/users/123456/nickname/98765")

      documenter.documentRequest(req, ".*([0-9]{6})\\/nickname\\/([0-9]{5})".r("userId", "nickname"))

      documenter.swaggerDocs.swagger.definitions.keys.size shouldEqual 0
      documenter.swaggerDocs.swagger.paths.keys.size shouldEqual 1

      val expectedParams = Set(Parameter("path", "userId", None, Some("string")), Parameter("path", "nickname", None, Some("string")))
      val operations: Map[String, Operation] = documenter.swaggerDocs.swagger.paths("/users/123456/nickname/98765")

      operations("get").parameters shouldEqual expectedParams
    }

    "record response" in {
      val mockFile = mock[File]
      val documenter = Mockito.spy(new Documenter())
      val req = Get("/users")
      val responseEntity = HttpEntity("{}")
      val response = new HttpResponse(StatusCode.int2StatusCode(200), List(), responseEntity, HttpProtocols.`HTTP/1.0`)
      val expectedResponse = Response("OK")
      val getOperation = Operation(Set("json"), Set("json"), Set(), Map())
      val swagger = Swagger(Map(), Map("/users" -> Map("get" -> getOperation)),
        Some(""), Info("Test", ""))
      documenter.swaggerDocs.swagger = swagger
      Mockito.doReturn(mockFile).when(documenter).createFile("test")
      Mockito.doNothing().when(documenter).writeToFile(mockFile)

      documenter.saveResponse("test", req, response)

      Mockito.verify(documenter).createFile("test")
      Mockito.verify(documenter).writeToFile(mockFile)

      val operation: Operation = documenter.swaggerDocs.swagger.paths("/users")("get")
      operation.responses("200") shouldEqual expectedResponse
    }

    "record response that has a body" in {
      val mockFile = mock[File]
      val documenter = Mockito.spy(new Documenter())
      val req = Get("/users")
      val responseEntity = HttpEntity("{}")
      val response = new HttpResponse(StatusCode.int2StatusCode(200), List(), responseEntity, HttpProtocols.`HTTP/1.0`)
      val expectedResponse = Response("OK", Some(Schema("#/definitions/TestClass")))
      val getOperation = Operation(Set(), Set(), Set(), Map())
      val body = TestClass("123", 21, isTrue = false, List("bob"))
      val swagger = Swagger(Map(), Map("/users" -> Map("get" -> getOperation)),
        Some(""), Info("Test", ""))
      documenter.swaggerDocs.swagger = swagger
      Mockito.doReturn(mockFile).when(documenter).createFile("test")
      Mockito.doNothing().when(documenter).writeToFile(mockFile)

      documenter.saveResponse("test", req, response, Some(TestClass("456", 22, isTrue = true, List("Fred"))))

      val model: Definition = documenter.swaggerDocs.swagger.definitions("TestClass")
      model.properties.keys.size shouldEqual 4
      model.properties("id").`type` shouldBe "string"
      model.properties("value").`type` shouldBe "integer"
      model.properties("isTrue").`type` shouldBe "boolean"
      model.properties("someList").`type` shouldBe "array"
      val operation: Operation = documenter.swaggerDocs.swagger.paths("/users")("get")
      operation.responses("200") shouldEqual expectedResponse
    }
  }

}
