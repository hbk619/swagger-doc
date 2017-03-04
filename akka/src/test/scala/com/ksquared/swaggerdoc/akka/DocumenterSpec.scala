package com.ksquared.swaggerdoc.akka

import java.io.File

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
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
  implicit val testClassFormatter = jsonFormat4(TestClass)
  implicit val testOptionalClassFormatter = jsonFormat4(TestOptionalClass)

  "Documenter" should {

    "record request" in {
      val documenter = new Documenter()
      val body = TestClass("123", 21, false, List("bob"))
      val req = Post("/test", body)

      documenter.documentRequest(req, body)

      documenter.swaggerDocs.swagger.definitions.keys.size shouldEqual 1
      documenter.swaggerDocs.swagger.paths.size shouldEqual 1
      val model: Option[Definition] = documenter.swaggerDocs.swagger.definitions.get("TestClass")
      model.isDefined shouldBe true
      model.get.properties.keys.size shouldEqual 4
      model.get.properties("id").`type` shouldBe "string"
      model.get.properties("value").`type` shouldBe "integer"
      model.get.properties("isTrue").`type` shouldBe "boolean"
      model.get.properties("someList").`type` shouldBe "array"

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

      val operations: Map[String, Operation] = documenter.swaggerDocs.swagger.paths("/users/123456/nickname/98765")
      operations("get").parameters.size shouldEqual 2
      operations("get").parameters.head.`type`.get shouldEqual "string"
      operations("get").parameters.head.`in` shouldEqual "path"
      operations("get").parameters.head.name shouldEqual "userId"
      operations("get").parameters(1).`type`.get shouldEqual "string"
      operations("get").parameters(1).`in` shouldEqual "path"
      operations("get").parameters(1).name shouldEqual "nickname"
    }

    "record response" in {
      val mockFile = mock[File]
      val documenter = Mockito.spy(new Documenter())
      Mockito.doReturn(mockFile).when(documenter).createFile("test")
      Mockito.doNothing().when(documenter).writeToFile(mockFile)

      documenter.saveResponse("test")

      Mockito.verify(documenter).createFile("test")
      Mockito.verify(documenter).writeToFile(mockFile)
    }
  }

}
