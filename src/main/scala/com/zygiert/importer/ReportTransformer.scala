package com.zygiert.importer

import cats.data.{NonEmptyChain, ValidatedNec}
import cats.implicits._
import com.zygiert.importer.ImporterDefinition.ReportImporter
import com.zygiert.importer.Model.ReportRowRepresentation
import com.zygiert.persistence.Model.Event

import scala.jdk.StreamConverters._

object ReportTransformer {

  def process(fileContent: String, importer: ReportImporter[ReportRowRepresentation])
             (rowsToEventsFunction: List[ReportRowRepresentation] => ValidatedNec[String, List[Event]]): Either[NonEmptyChain[String], List[Event]] = {
    toRowRepresentations(fileContent, importer).toEither
      .flatMap(rows => rowsToEventsFunction(rows).toEither)
  }

  private def toRowRepresentations(fileContent: String, importer: ReportImporter[ReportRowRepresentation]): ValidatedNec[String, List[ReportRowRepresentation]] = {
    fileContent.lines().toScala(LazyList)
      .drop(1)
      .filter(!_.isBlank)
      .map(importer.toReportRowRepresentation)
      .toList
      .sequence
  }

}
