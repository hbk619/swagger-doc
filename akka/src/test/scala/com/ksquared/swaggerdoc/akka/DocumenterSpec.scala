package com.ksquared.swaggerdoc.akka

import java.io.File

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.ksquared.swaggerdoc.models.{Api, Model}
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import spray.json.DefaultJsonProtocol._
import spray.json._

class DocumenterSpec extends WordSpec with Matchers
  with ScalatestRouteTest with SprayJsonSupport
  with DefaultJsonProtocol with MockitoSugar {

  case class TestClass(id: String, value: Int, isTrue: Boolean, someList: List[String])
  implicit val testClassFormatter = jsonFormat4(TestClass)

  "Documenter" should {

    "record request" in {
      val documenter = new Documenter()
      val body = TestClass("123", 21, false, List("bob"))
      val req = Post("/test", body)

      documenter.documentRequest(req, body)

      documenter.swaggerDocs.swagger.models.keys.size shouldEqual 1
      documenter.swaggerDocs.swagger.api.size shouldEqual 1
      val model: Option[Model] = documenter.swaggerDocs.swagger.models.get("TestClass")
      model.isDefined shouldBe true
      model.get.properties.keys.size shouldEqual 4
      model.get.properties.get("id").get.`type` shouldBe "string"
      model.get.properties.get("value").get.`type` shouldBe "integer"
      model.get.properties.get("isTrue").get.`type` shouldBe "boolean"
      model.get.properties.get("someList").get.`type` shouldBe "array"

      val api: Api = documenter.swaggerDocs.swagger.api.head
      api.path shouldEqual "/test"
      api.operations.size shouldEqual 1
      api.operations.head.method shouldEqual "POST"
      api.operations.head.parameters.size shouldEqual 1
      api.operations.head.parameters.head.`type` shouldEqual "TestClass"
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
