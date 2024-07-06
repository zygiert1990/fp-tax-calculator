package com.zygiert.importer

import cats.data.ValidatedNec
import com.zygiert.importer.Model.RowRepresentation
import com.zygiert.model.Model.{Broker, Currency}
import com.zygiert.persistence.Model.Event
import org.http4s.Charset

import java.nio.charset.{Charset => JCharset}

object ImporterDefinition {

  sealed trait ReportImporter[T<:RowRepresentation] {
    val charset: JCharset = Charset.`UTF-8`.nioCharset
    val validHeader: String

    def toReportRowRepresentation(reportRow: String): ValidatedNec[String, T]
  }
  trait MultipleCurrency[T<:RowRepresentation] extends ReportImporter[T] {
    def toEvents(rows: List[T], broker: Broker): ValidatedNec[String, List[Event]]
  }
  trait SingleCurrency[T<:RowRepresentation] extends ReportImporter[T] {
    def toEvents(rows: List[T], broker: Broker, currency: Currency): ValidatedNec[String, List[Event]]
  }

}
