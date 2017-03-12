package com.ksquared.swaggerdoc.models

case class Property(`type`: String)

case class Schema($ref: String)

case class Parameter(`in`: String, name: String, schema: Option[Schema], `type`: Option[String])

case class Definition(`type`: String, properties: Map[String, Property])

case class Response(description: String, schema: Option[Schema] = None)

case class Operation(consumes: Set[String], produces: Set[String],
                     parameters: Set[Parameter],
                     responses: Map[String, Response])

case class Info(title: String, version: String)

case class Swagger(definitions: Map[String, Definition],
                   paths: Map[String, Map[String, Operation]],
                   basePath: Option[String],
                   info: Info = Info("An app", "0.0.1"),
                   swagger: String = "2.0")

class SwaggerDocs(apiVersion: String,
                  basePath: Option[String] = None) {

  var swagger = Swagger(Map(), Map(),
    basePath, Info("Test", apiVersion))

  def addOperation(url: String, method: String, operation: Operation) = {
    if (!swagger.paths.contains(url)) {
      val paths = swagger.paths + (url -> Map(method -> operation))
      swagger = swagger.copy(paths = paths)
    } else if (!swagger.paths(url).contains(method)) {
      val operations = swagger.paths(url) + (method -> operation)
      val paths = swagger.paths + (url -> operations)
      swagger = swagger.copy(paths = paths)
    } else {
      val existingOperation = swagger.paths(url)(method)
      val newOperation = Operation(
        existingOperation.consumes ++ operation.consumes,
        existingOperation.produces ++ operation.produces,
        existingOperation.parameters ++ operation.parameters,
        existingOperation.responses ++ operation.responses
      )
      val operations = swagger.paths(url) + (method -> newOperation)
      val paths = swagger.paths + (url -> operations)
      swagger = swagger.copy(paths = paths)
    }
  }

  def addDefinition(name: String, model: Definition): Unit = {
    val definitions = swagger.definitions + (name -> model)
    swagger = swagger.copy(definitions = definitions)
  }

  def addPaths(paths: Map[String, Map[String, Operation]]) = {
    for {
      (url, operations) <- paths
      (method, operation) <- operations
    } yield addOperation(url, method, operation)
  }

  def addDefinitions(definitions: Map[String, Definition]) = {
    for {
      (defName, definition) <- definitions
    } yield addDefinition(defName, definition)
  }

  def addResponse(url: String, method: String, status: Int, response: Response) = {
    val existingResponses = swagger.paths(url)(method).responses
    val newResponses = existingResponses + (status.toString -> response)
    val operation = swagger.paths(url)(method).copy(responses = newResponses)
    addOperation(url, method, operation)
  }
}