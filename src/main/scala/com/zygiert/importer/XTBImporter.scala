package com.zygiert.importer

import cats.data.ValidatedNec
import cats.implicits._
import com.zygiert.importer.ImporterDefinition.SingleCurrency
import com.zygiert.importer.Model.XTBReportRow
import com.zygiert.model.Model.{Broker, Currency, Instrument}
import com.zygiert.persistence.Model._

import scala.util.Try

object XTBImporter extends SingleCurrency[XTBReportRow] {

  private val dateTimeFormat = "dd.MM.yyyy HH:mm:ss"
  private val rowValuesSeparator = ";"

  override val validHeader: String = "ID;Type;Time;Symbol;Comment;Amount"

  override def toEvents(rows: List[XTBReportRow], broker: Broker, currency: Currency): ValidatedNec[String, List[Event]] =
    rows.traverse(row => toEvent(row, broker, currency).toValidatedNec)

  override def toReportRowRepresentation(reportRow: String): ValidatedNec[String, XTBReportRow] = {
    val values = reportRow.split(rowValuesSeparator)
    (Validations.canParseToLocalDateTime(values(2), dateTimeFormat), Validations.canParseToBigDecimal(values(5)))
      .mapN { (dateTime, amount) =>
        XTBReportRow(values(0), values(1), dateTime, values(3), values(4), amount.abs)
      }
  }

  private def toEvent(row: XTBReportRow, broker: Broker, currency: Currency): Either[String, Event] = {
    row.operationType match {
        case "Deposit"             => Right(DepositMade(row.dateTime, broker, row.amount, currency))
        case "Withdrawal"          => Right(WithdrawalDone(row.dateTime, broker, row.amount, currency))
        case "Stocks/ETF purchase" => tryExtractAmount(row).map(amount => AssetBought(row.dateTime, broker, Instrument(row.instrumentSymbol), amount, row.amount, 0, currency))
        case "Stocks/ETF sale"     => tryExtractAmount(row).map(amount => AssetSold(row.dateTime, broker, Instrument(row.instrumentSymbol), amount, row.amount, 0, currency))
        case "Dividend"            => Right(DividendPaid(row.dateTime, broker, Instrument(row.instrumentSymbol), row.amount, currency))
        case "Withholding tax"     => Right(WitholdTaxCharged(row.dateTime, broker, row.amount, currency))
        case _                     => Left(s"Can not recognize operation type for row: $row")
      }
  }

  private def tryExtractAmount(row: XTBReportRow): Either[String, Int] =
    Either.fromTry(Try(row.comment.split("BUY")(1).split("@")(0).trim().toInt))
      .leftMap(t => s"Can not extract amount from row: ${row.toString} due to: ${t.getMessage}")
}
