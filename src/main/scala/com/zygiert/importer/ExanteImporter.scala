package com.zygiert.importer

import cats.data.Validated.Invalid
import cats.data.{NonEmptyChain, ValidatedNec}
import cats.implicits._
import com.zygiert.importer.ImporterDefinition.MultipleCurrency
import com.zygiert.importer.Model.ExanteReportRow
import com.zygiert.model.Model.Broker
import com.zygiert.persistence.Model.Event
import org.http4s.Charset

import java.nio.charset.{Charset => JCharset}

object ExanteImporter extends MultipleCurrency[ExanteReportRow] {

  override val charset: JCharset = Charset.`UTF-16`.nioCharset
  private val dateTimeFormat = "yyyy-MM-dd HH:mm:ss"
  private val rowValuesSeparator = "\t"

  override def toEvents(rows: List[ExanteReportRow], broker: Broker): ValidatedNec[String, List[Event]] = {
    Invalid(NonEmptyChain("Not implemented yet!"))
  }

  override def toReportRowRepresentation(reportRow: String): ValidatedNec[String, ExanteReportRow] = {
    val values = normalize(reportRow)
    (Validations.canParseToLocalDateTime(values(5), dateTimeFormat), Validations.canParseToBigDecimal(values(6)))
      .mapN { (dateTime, value) =>
        ExanteReportRow(values(0), values(2), values(4), dateTime, value.abs, values(7), values(9))
      }
  }

  private def normalize(reportRow: String): Array[String] = {
    reportRow.split(rowValuesSeparator).map(value => value.replace("\"", ""))
  }
}
