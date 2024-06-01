package importer

import cats.data.ValidatedNec
import importer.Model.{ExanteReportRow, RowRepresentation}
import model.Model.Currency
import org.http4s.Charset
import persistence.Model.Event

import java.nio.charset.{Charset => JCharset}

object Importer {

  sealed trait Command
  case class Deposit() extends Command

  sealed trait ReportImporter[T<:RowRepresentation] {
    val charset: JCharset = Charset.`UTF-8`.nioCharset

    def toReportRowRepresentation(reportRow: String): ValidatedNec[String, T]
  }
  trait MultipleCurrency[T<:RowRepresentation] extends ReportImporter[T] {
    def toEvents(rows: List[T]): ValidatedNec[String, List[Event]]
  }
  trait SingleCurrency[T<:RowRepresentation] extends ReportImporter[T] {
    def toEvents(rows: List[T], currency: Currency): ValidatedNec[String, List[Event]]
  }

  object ExanteImporter extends MultipleCurrency[ExanteReportRow] {
    override val charset: JCharset = Charset.`UTF-16`.nioCharset

    override def toEvents(rows: List[ExanteReportRow]): ValidatedNec[String, List[Event]] = ???

    override def toReportRowRepresentation(reportRow: String): ValidatedNec[String, ExanteReportRow] = {
      val values = reportRow.split("\\t")
      ExanteReportRow(values(1))
      ???
    }
  }

}
