package com.ksquared.swaggerdoc.models

case class Property(`type`: String)

case class Parameter(`type`: String, paramType: String, name: String)

case class Model(name: String, properties: Map[String, Property])

case class Operation(method: String, parameters: Seq[Parameter])

case class Api(path: String, operations: Seq[Operation])

case class Swagger(apis: List[Api],
                   models: Map[String, Model],
                   apiVersion: String,
                   basePath: Option[String],
                   produces: Option[List[String]],
                   resourcePath: Option[String])

class SwaggerDocs(apiVersion: String,
                  resourcePath: Option[String] = None,
                  produces: Option[List[String]] = None) {

  var swagger = Swagger(List(), Map(),
    apiVersion, Some("/"),
    produces, resourcePath)

  def addApi(api: Api) = {
    if (!swagger.apis.contains(api)) {
      val apis = api :: swagger.apis
      swagger = swagger.copy(apis = apis)
    }
  }

  def addModel(model: Model): Unit = {
    if (swagger.models.get(model.name).isEmpty) {
      val models = swagger.models + (model.name -> model)
      swagger = swagger.copy(models = models)
    }
  }
}