package com.ksquared.swaggerdoc.plugin

import java.io.File

import org.apache.commons.io.FileUtils
import sbt.{Def, SettingKey, TaskKey}

object Plugin extends sbt.AutoPlugin with DocGenerator {

  val defaultInputLocation: String = System.getProperty("user.dir") + "/restdoc/generated"
  val defaultOutputLocation: String = System.getProperty("user.dir") + "/restdoc/generated"

  override def trigger = allRequirements

  object autoImport {
    lazy val swaggerDocTask = TaskKey[Unit]("swagger-doc", "Creates swagger docs json")
    lazy val swaggerDocConfig = SettingKey[Map[String, String]]("swagger-doc-config", "Base config for swagger doc")
  }

  import autoImport._

  def swaggerDocGenerator = Def.task {
    val config: Map[String, String] = swaggerDocConfig.value
    val location = config.getOrElse("output", Plugin.defaultOutputLocation)
    val outputFile = new File(location)

    val baseUrl = config.getOrElse("baseUrl", "/")
    val apiVersion = config.getOrElse("apiVersion", "0.0.0")
    val inputFile = new File(config.getOrElse("input", Plugin.defaultInputLocation))

    val generatedDoc: String = generateDoc(inputFile, apiVersion, baseUrl)
    FileUtils.writeStringToFile(outputFile, generatedDoc, "UTF-8", false)
    generatedDoc
  }

  override lazy val projectSettings = Seq(
    swaggerDocConfig := Map(
      "title" -> "Documentation for an api",
      "baseUrl" -> "/",
      "apiVersion" -> "0.0.1",
      "output" -> defaultOutputLocation,
      "input" -> defaultInputLocation
    ),
    swaggerDocTask := swaggerDocGenerator.value
  )
}