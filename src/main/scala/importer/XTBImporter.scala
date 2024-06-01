package importer

import cats.data.ValidatedNec
import cats.implicits._
import importer.Importer.SingleCurrency
import importer.Model.XTBReportRow
import model.Model.{Broker, Currency, Instrument}
import persistence.Model._

object XTBImporter extends SingleCurrency[XTBReportRow] {

  override def toEvents(rows: List[XTBReportRow], currency: Currency): ValidatedNec[String, List[Event]] = {
    rows.traverse(row => toEvent(row, currency).toValidatedNec)
  }

  override def toReportRowRepresentation(reportRow: String): ValidatedNec[String, XTBReportRow] = {
    val values = reportRow.split(";")
    (Validations.canParseToLocalDateTime(values(2), "dd.MM.yyyy HH:mm:ss"), Validations.canParseToBigDecimal(values(5)))
      .mapN { (dateTime, amount) =>
        XTBReportRow(values(1), dateTime, values(3), values(4), amount.abs)
      }
  }

  private def toEvent(row: XTBReportRow, currency: Currency): Either[String, Event] = {
    val xtb = Broker("XTB")
    row.operationType match {
        case "Deposit"             => Right(DepositMade(row.dateTime, xtb, row.amount, currency))
        case "Withdrawal"          => Right(WithdrawalDone(row.dateTime, xtb, row.amount, currency))
        case "Stocks/ETF purchase" => tryExtractAmount(row).map(amount => AssetBought(row.dateTime, xtb, Instrument(row.symbol), amount, row.amount, 0, currency))
        case "Stocks/ETF sale"     => tryExtractAmount(row).map(amount => AssetSold(row.dateTime, xtb, Instrument(row.symbol), amount, row.amount, 0, currency))
        case "Dividend"            => Right(DividendPaid(row.dateTime, xtb, Instrument(row.symbol), row.amount, currency))
        case "Withholding tax"     => Right(WitholdTaxCharged(row.dateTime, xtb, row.amount, currency))
        case _                     => Left(s"Can not recognize operation type for row: ${row.toString}")
      }
  }

  private def tryExtractAmount(row: XTBReportRow): Either[String, Int] =
    Either.fromOption(row.comment.split("BUY")(1).split("@")(0).trim().toIntOption, s"Can not extract amount from row: ${row.toString}")
}
