package com.ksquared.swaggerdoc.models

import org.scalatest.{Matchers, WordSpec}

class SwaggerDocsSpec extends WordSpec with Matchers {

  "Swagger doc" when {
    "addOperation" should {
      "add an operation if it does not exist" in {
        val swaggerDoc = new SwaggerDocs("0.0.1", Some("/base"))
        val operation = Operation(Set("json"), Set("json"), Set(), Map())
        swaggerDoc.addOperation("/test", "GET", operation)

        swaggerDoc.swagger.paths.get("/test").isDefined shouldBe true
        swaggerDoc.swagger.paths("/test") should contain key "GET"
        swaggerDoc.swagger.paths("/test")("GET") shouldEqual operation
      }

      "update an operation if it exists" in {
        val swaggerDoc = new SwaggerDocs("0.0.1", Some("/base"))
        val operation = Operation(Set("json"), Set("json"), Set(), Map())
        val operation2 = Operation(Set("json"), Set("json"), Set(Parameter("body", "test", None, Some("string"))), Map())
        swaggerDoc.addOperation("/test", "GET", operation)
        swaggerDoc.addOperation("/test", "GET", operation2)

        swaggerDoc.swagger.paths should contain key "/test"
        swaggerDoc.swagger.paths("/test") should contain key "GET"
        swaggerDoc.swagger.paths("/test")("GET") shouldEqual operation2
      }
    }

    "addDefinition" should {
      "add a new definition" in {
        val swaggerDoc = new SwaggerDocs("0.0.1", Some("/base"))
        val definition = Definition("object", Map())

        swaggerDoc.addDefinition("someName", definition)

        swaggerDoc.swagger.definitions should contain key "someName"
        swaggerDoc.swagger.definitions("someName") shouldEqual definition
      }

      "update a definition" in {
        val swaggerDoc = new SwaggerDocs("0.0.1", Some("/base"))
        val definition = Definition("object", Map())
        val definition1 = Definition("object", Map("test" -> Property("array")))

        swaggerDoc.addDefinition("someName", definition)
        swaggerDoc.addDefinition("someName", definition1)

        swaggerDoc.swagger.definitions should contain key "someName"
        swaggerDoc.swagger.definitions("someName") shouldEqual definition1
      }
    }

    "addPath" should {
      "add new paths" in {
        val swaggerDoc = new SwaggerDocs("0.0.1", Some("/base"))
        val operation = Operation(Set("json"), Set("json"), Set(), Map())
        val operation2 = Operation(Set("json"), Set("json"), Set(Parameter("body", "test", None, Some("string"))), Map())

        val paths = Map("/test" -> Map("GET" -> operation, "POST" -> operation2))

        swaggerDoc.addPaths(paths)

        swaggerDoc.swagger.paths shouldEqual paths
      }

      "update existing paths with new method" in {
        val swaggerDoc = new SwaggerDocs("0.0.1", Some("/base"))
        val operation = Operation(Set("json"), Set("json"), Set(), Map())
        val operation2 = Operation(Set("json"), Set("json"), Set(Parameter("body", "test", None, Some("string"))), Map())
        val operation3 = Operation(Set("xml"), Set("xml"), Set(Parameter("body", "test", None, Some("string"))), Map())

        val paths = Map("/test" -> Map("GET" -> operation, "POST" -> operation2))
        val paths1 = Map("/test" -> Map("PUT" -> operation3))
        val expectedPaths = Map("/test" -> Map("GET" -> operation, "POST" -> operation2, "PUT" -> operation3))

        swaggerDoc.addPaths(paths)
        swaggerDoc.addPaths(paths1)

        swaggerDoc.swagger.paths shouldEqual expectedPaths
      }

      "update existing method with new data" in {
        val swaggerDoc = new SwaggerDocs("0.0.1", Some("/base"))
        val operation = Operation(Set("json"), Set("json"), Set(), Map())
        val operation2 = Operation(Set("json"), Set("json"), Set(Parameter("body", "test", None, Some("string"))), Map("400" -> Response("Bad")))
        val operation3 = Operation(Set("xml"), Set("xml"), Set(Parameter("body", "test", None, Some("string"))), Map("200" -> Response("OK")))
        val expectedOperation = Operation(Set("json", "xml"), Set("json", "xml"), Set(Parameter("body", "test", None, Some("string"))), Map("200" -> Response("OK"), "400" -> Response("Bad")))
        val paths = Map("/test" -> Map("GET" -> operation, "POST" -> operation2))
        val paths1 = Map("/test" -> Map("POST" -> operation3))
        val expectedPaths = Map("/test" -> Map("GET" -> operation, "POST" -> expectedOperation))

        swaggerDoc.addPaths(paths)
        swaggerDoc.addPaths(paths1)

        swaggerDoc.swagger.paths shouldEqual expectedPaths
      }
    }

    "addDefinitions" should {
      "add a new definition" in {
        val swaggerDoc = new SwaggerDocs("0.0.1", Some("/base"))
        val definition = Definition("object", Map())

        swaggerDoc.addDefinitions(Map("someName" -> definition))

        swaggerDoc.swagger.definitions should contain key "someName"
        swaggerDoc.swagger.definitions("someName") shouldEqual definition
      }

      "update definitions" in {
        val swaggerDoc = new SwaggerDocs("0.0.1", Some("/base"))
        val definition = Definition("object", Map())
        val definition1 = Definition("object", Map("test" -> Property("array")))
        val definition2 = Definition("object", Map("test" -> Property("array")))

        swaggerDoc.addDefinitions(Map("someName" -> definition, "otherDef" -> definition2))
        swaggerDoc.addDefinitions(Map("someName" -> definition1))

        swaggerDoc.swagger.definitions should contain key "someName"
        swaggerDoc.swagger.definitions should contain key "otherDef"
        swaggerDoc.swagger.definitions("someName") shouldEqual definition1
        swaggerDoc.swagger.definitions("otherDef") shouldEqual definition2
      }
    }

    "addResponse" should {
      "add response to existing operation" in {
        val swaggerDoc = new SwaggerDocs("0.0.1", Some("/base"))
        val operation = Operation(Set("json"), Set("json"), Set(), Map())
        val operation2 = Operation(Set("json"), Set("json"), Set(Parameter("body", "test", None, Some("string"))), Map())
        val operation3 = Operation(Set("xml"), Set("xml"), Set(Parameter("body", "test", None, Some("string"))), Map())

        val paths = Map("/test" -> Map("GET" -> operation, "POST" -> operation2), "/test2" -> Map("PUT" -> operation3))
        val expectedOperation = Operation(Set("json"), Set("json"), Set(), Map("400" -> Response("Bad request")))

        swaggerDoc.swagger = swaggerDoc.swagger.copy(paths = paths)

        swaggerDoc.addResponse("/test", "GET", 400, Response("Bad request"))

        swaggerDoc.swagger.paths("/test")("GET") shouldEqual expectedOperation
        swaggerDoc.swagger.paths("/test")("POST") shouldEqual operation2
        swaggerDoc.swagger.paths("/test2")("PUT") shouldEqual operation3
      }
    }
  }
}
