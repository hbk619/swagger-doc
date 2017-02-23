package com.ksquared.swaggerdoc.models

case class Property(`type`: String)

case class Parameter(`type`: String)

case class Model(name: String, properties: Map[String, Property])

case class Operation(method: String, parameters: Seq[Parameter])

case class Api(path: String, operations: Seq[Operation])

case class Swagger(api: List[Api], models: Map[String, Model])

class SwaggerDocs {
  var swagger = Swagger(List(), Map())

  def addApi(api: Api) = {
    if (!swagger.api.contains(api)) {
      val apis = api :: swagger.api
      swagger = swagger.copy(api = apis)
    }
  }

  def addModel(model: Model): Unit = {
    if (swagger.models.get(model.name).isEmpty) {
      val models = swagger.models + (model.name -> model)
      swagger = swagger.copy(models = models)
    }
  }
}