package com.ksquared.swaggerdoc.plugin

import java.io.File
import java.util

import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}

import scala.io.Source

class DocGeneratorSpec extends WordSpec with Matchers
  with DocGenerator with MockitoSugar {

  "DocGenerator" should {
    "sort stuff out" in {
      val mockFile = mock[File]
      val mockFile2 = mock[File]
      val mockFile3 = mock[File]
      val mockDoc = Mockito.spy(this)
      val fileList = new util.ArrayList[File]()
      fileList.add(mockFile2)
      fileList.add(mockFile3)
      val swaggerFile: String = Source.fromURL(getClass.getResource("/test-data/swagger.json"), "UTF-8").mkString
      val swaggerFile1: String = Source.fromURL(getClass.getResource("/test-data/swagger1.json"), "UTF-8").mkString
      val expected: String = Source.fromURL(getClass.getResource("/test-data/expectedOutput.json"), "UTF-8").mkString

      Mockito.doReturn(fileList).when(mockDoc).getFiles(mockFile, "json")
      Mockito.doReturn(swaggerFile).when(mockDoc).getFileContents(mockFile2)
      Mockito.doReturn(swaggerFile1).when(mockDoc).getFileContents(mockFile3)

      val docString = mockDoc.generateDoc(mockFile, "0.0.1", "/")

      docString shouldBe expected
    }
  }
}
