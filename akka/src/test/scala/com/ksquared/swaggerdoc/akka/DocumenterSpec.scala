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
  case class ComplexClass(id: String, testClass: TestClass)
  case class ComplexOptionalClass(id: String, testClass: Option[TestClass])
  case class TestOptionalClass(id: Option[String], value: Option[Int], isTrue: Option[Boolean], someList: Option[List[String]])
  implicit val testClassFormatter: RootJsonFormat[TestClass] = jsonFormat4(TestClass)
  implicit val complexClassFormatter: RootJsonFormat[ComplexClass] = jsonFormat2(ComplexClass)
  implicit val complexOptionalClassFormatter: RootJsonFormat[ComplexOptionalClass] = jsonFormat2(ComplexOptionalClass)
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
      model.properties("id").`type`.get shouldBe "string"
      model.properties("value").`type`.get shouldBe "integer"
      model.properties("isTrue").`type`.get shouldBe "boolean"
      model.properties("someList").`type`.get shouldBe "array"

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
      model.get.properties("id").`type`.get shouldBe "string"
      model.get.properties("value").`type`.get shouldBe "integer"
      model.get.properties("isTrue").`type`.get shouldBe "boolean"
      model.get.properties("someList").`type`.get shouldBe "array"

      val operations: Map[String, Operation] = documenter.swaggerDocs.swagger.paths("/test")
      operations.get("post").isDefined shouldEqual true
      operations("post").parameters.size shouldEqual 1
      operations("post").parameters.head.schema.get.$ref shouldEqual "#/definitions/TestOptionalClass"
    }

    "record complex definitions request" in {
      val documenter = new Documenter()
      val testClass = TestClass("123", 21, isTrue = false, List("bob"))
      val body = ComplexClass("987", testClass)
      val req = Post("/test", body)

      documenter.documentRequest(req, body)

      documenter.swaggerDocs.swagger.definitions.keys.size shouldEqual 2
      val complexModel: Definition = documenter.swaggerDocs.swagger.definitions("ComplexClass")
      complexModel.properties.keys.size shouldEqual 2
      complexModel.properties("id").`type`.get shouldBe "string"
      complexModel.properties("testClass").$ref.get shouldBe "#/definitions/TestClass"

      val model: Definition = documenter.swaggerDocs.swagger.definitions("TestClass")
      model.properties.keys.size shouldEqual 4
      model.properties("id").`type`.get shouldBe "string"
      model.properties("value").`type`.get shouldBe "integer"
      model.properties("isTrue").`type`.get shouldBe "boolean"
      model.properties("someList").`type`.get shouldBe "array"

      val operations: Map[String, Operation] = documenter.swaggerDocs.swagger.paths("/test")
      operations.size shouldEqual 1
      operations.get("post").isDefined shouldEqual true
      operations("post").parameters.head.schema.get.$ref shouldEqual "#/definitions/ComplexClass"
    }

    "record complex optional definitions request" in {
      val documenter = new Documenter()
      val testClass = TestClass("123", 21, isTrue = false, List("bob"))
      val body = ComplexOptionalClass("987", Some(testClass))
      val req = Post("/test", body)

      documenter.documentRequest(req, body)

      documenter.swaggerDocs.swagger.definitions.keys.size shouldEqual 2
      val complexModel: Definition = documenter.swaggerDocs.swagger.definitions("ComplexOptionalClass")
      complexModel.properties.keys.size shouldEqual 2
      complexModel.properties("id").`type`.get shouldBe "string"
      complexModel.properties("testClass").$ref.get shouldBe "#/definitions/TestClass"

      val model: Definition = documenter.swaggerDocs.swagger.definitions("TestClass")
      model.properties.keys.size shouldEqual 4
      model.properties("id").`type`.get shouldBe "string"
      model.properties("value").`type`.get shouldBe "integer"
      model.properties("isTrue").`type`.get shouldBe "boolean"
      model.properties("someList").`type`.get shouldBe "array"

      val operations: Map[String, Operation] = documenter.swaggerDocs.swagger.paths("/test")
      operations.size shouldEqual 1
      operations.get("post").isDefined shouldEqual true
      operations("post").parameters.head.schema.get.$ref shouldEqual "#/definitions/ComplexOptionalClass"
    }

    "record path regex request" in {
      val documenter = new Documenter()
      val req = Get("/users/123456/nickname/98765")

      documenter.documentRequest(req, ".*([0-9]{6})\\/nickname\\/([0-9]{5})".r("userId", "nickname"))

      documenter.swaggerDocs.swagger.definitions.keys.size shouldEqual 0
      documenter.swaggerDocs.swagger.paths.keys.size shouldEqual 1

      val expectedParams = Set(Parameter("path", "userId", None, Some("string")), Parameter("path", "nickname", None, Some("string")))
      val operations: Map[String, Operation] = documenter.swaggerDocs.swagger.paths("/users/{userId}/nickname/{nickname}")

      operations("get").parameters shouldEqual expectedParams
    }

    "record path and body request" in {
      val documenter = new Documenter()
      val body = TestClass("123", 21, isTrue = false, List("bob"))
      val req = Post("/users/123456", body)

      documenter.documentRequest(req, body, ".*\\/users\\/([0-9]{6})$".r("userId"))

      documenter.swaggerDocs.swagger.definitions.keys.size shouldEqual 1
      documenter.swaggerDocs.swagger.paths.size shouldEqual 1
      val model: Definition = documenter.swaggerDocs.swagger.definitions("TestClass")
      model.properties.keys.size shouldEqual 4
      model.properties("id").`type`.get shouldBe "string"
      model.properties("value").`type`.get shouldBe "integer"
      model.properties("isTrue").`type`.get shouldBe "boolean"
      model.properties("someList").`type`.get shouldBe "array"

      val expectedParams = Set(Parameter("body", "TestClass", Some(Schema("#/definitions/TestClass")), None), Parameter("path", "userId", None, Some("string")))

      val operations: Map[String, Operation] = documenter.swaggerDocs.swagger.paths("/users/{userId}")
      operations.size shouldEqual 1
      operations.get("post").isDefined shouldEqual true
      operations("post").parameters shouldEqual expectedParams
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
        Set(), Some(""), Info("Test", ""))
      documenter.swaggerDocs.swagger = swagger
      Mockito.doReturn(mockFile).when(documenter).createFile("test")
      Mockito.doNothing().when(documenter).writeToFile(mockFile)

      documenter.saveResponse(ResponseDetails("test", req, response, "/users"))

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
        Set(), Some(""), Info("Test", ""))
      documenter.swaggerDocs.swagger = swagger
      Mockito.doReturn(mockFile).when(documenter).createFile("test")
      Mockito.doNothing().when(documenter).writeToFile(mockFile)

      documenter.saveResponse(ResponseDetails("test", req, response, "/users", Some(TestClass("456", 22, isTrue = true, List("Fred")))))

      val model: Definition = documenter.swaggerDocs.swagger.definitions("TestClass")
      model.properties.keys.size shouldEqual 4
      model.properties("id").`type`.get shouldBe "string"
      model.properties("value").`type`.get shouldBe "integer"
      model.properties("isTrue").`type`.get shouldBe "boolean"
      model.properties("someList").`type`.get shouldBe "array"
      val operation: Operation = documenter.swaggerDocs.swagger.paths("/users")("get")
      operation.responses("200") shouldEqual expectedResponse
    }
  }

}
