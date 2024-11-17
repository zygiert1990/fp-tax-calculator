package com.zygiert.importer

import cats.data.Validated.Valid
import com.zygiert.importer.Model.XTBReportRow
import com.zygiert.importer.XTBImporter.*
import com.zygiert.model.Model.{Broker, Currency, Instrument}
import com.zygiert.persistence.Model.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.time.LocalDateTime

class XTBImporterTest extends AnyFunSpec with Matchers {

  private val id = "333299201"
  private val dateTime = LocalDateTime.of(2022, 12, 15, 16, 55, 21)
  private val currencySymbol = "USD"
  private val currency = Currency(currencySymbol)
  private val broker = Broker("XTB")
  private val amount = BigDecimal(140.68)
  private val instrumentSymbol = "PYPL.US"
  private val instrument = Instrument(instrumentSymbol)
  private val validComment = "OPEN BUY 2 @ 70.34"
  private val stocksEtfPurchase = "Stocks/ETF purchase"
  private val stocksEtfSale = "Stocks/ETF sale"

  describe("XTBImporter") {
    describe("mapping to report row representation") {
      describe("invalid cases") {
        it("should return invalid when cannot parse date time") {
          // given
          val row = "333299201;Stocks/ETF purchase;15.12.2022T16:55:21;PYPL.US;OPEN BUY 2 @ 70.34;-140.68"
          // when
          val result = toReportRowRepresentation(row)
          // then
          result.isInvalid shouldBe true
        }

        it("should return invalid when cannot parse amount") {
          // given
          val row = "333299201;Stocks/ETF purchase;15.12.2022 16:55:21;PYPL.US;OPEN BUY 2 @ 70.34;-140.68T"
          // when
          val result = toReportRowRepresentation(row)
          // then
          result.isInvalid shouldBe true
        }
      }
      describe("valid cases") {
        it("should parse row with negative amount") {
          // given
          val row = "333299201;Stocks/ETF purchase;15.12.2022 16:55:21;PYPL.US;OPEN BUY 2 @ 70.34;-140.68"
          // when
          val result = toReportRowRepresentation(row)
          // then
          result shouldEqual Valid(stockPurchase)
        }

        it("should parse row with positive amount") {
          // given
          val row = "333299201;Stocks/ETF purchase;15.12.2022 16:55:21;PYPL.US;OPEN BUY 2 @ 70.34;140.68"
          // when
          val result = toReportRowRepresentation(row)
          // then
          result shouldEqual Valid(stockPurchase)
        }
      }
    }
    describe("mapping to events") {
      describe("successful") {
        it("should create DepositMade event") {
          // given
          val rows = List(deposit)
          // when
          val result = toEvents(rows, broker, currency)
          // then
          result shouldEqual Valid(List(DepositMade(dateTime, broker, amount, currency)))
        }

        it("should create WithdrawalDone event") {
          // given
          val rows = List(withdrawal)
          // when
          val result = toEvents(rows, broker, currency)
          // then
          result shouldEqual Valid(List(WithdrawalDone(dateTime, broker, amount, currency)))
        }

        it("should create AssetBought event") {
          // given
          val rows = List(stockPurchase)
          // when
          val result = toEvents(rows, broker, currency)
          // then
          result shouldEqual Valid(List(AssetBought(dateTime, broker, instrument, 2, amount, 0, currency)))
        }

        it("should create AssetSold event") {
          // given
          val rows = List(stockSale)
          // when
          val result = toEvents(rows, broker, currency)
          // then
          result shouldEqual Valid(List(AssetSold(dateTime, broker, instrument, 2, amount, 0, currency)))
        }

        it("should create DividendPaid event") {
          // given
          val rows = List(dividend)
          // when
          val result = toEvents(rows, broker, currency)
          // then
          result shouldEqual Valid(List(DividendPaid(dateTime, broker, instrument, amount, currency)))
        }

        it("should create WitholdTaxCharged event") {
          // given
          val rows = List(witholdTax)
          // when
          val result = toEvents(rows, broker, currency)
          // then
          result shouldEqual Valid(List(WithholdTaxCharged(dateTime, broker, amount, currency)))
        }

        it("should create multiple events") {
          // given
          val rows = List(deposit, stockPurchase, dividend, witholdTax, stockSale, withdrawal)
          // when
          val result = toEvents(rows, broker, currency)
          // then
          result.isValid shouldBe true
          result.map(events => events.size shouldEqual 6)
        }
      }
      describe("unsuccessful") {
        describe("Stocks/ETF purchase") {
          it("should fails when lack of 'BUY' in comment") {
            // given
            val rows = List(stockPurchaseBase("OPEN 2 @ 70.34"))
            // when
            val result = toEvents(rows, broker, currency)
            // then
            result.isInvalid shouldBe true
          }

          it("should fails when lack of '@' in comment") {
            // given
            val rows = List(stockPurchaseBase("OPEN BUY 2  70.34"))
            // when
            val result = toEvents(rows, broker, currency)
            // then
            result.isInvalid shouldBe true
          }
        }
        describe("Stocks/ETF sale") {
          it("should fails when lack of 'BUY' in comment") {
            // given
            val rows = List(stockSaleBase("OPEN 2 @ 70.34"))
            // when
            val result = toEvents(rows, broker, currency)
            // then
            result.isInvalid shouldBe true
          }

          it("should fails when lack of '@' in comment") {
            // given
            val rows = List(stockSaleBase("OPEN BUY 2  70.34"))
            // when
            val result = toEvents(rows, broker, currency)
            // then
            result.isInvalid shouldBe true
          }
        }
        it("should fails when unknown operation type") {
          // given
          val rows = List(rowRepresentation("unknown")(validComment))
          // when
          val result = toEvents(rows, broker, currency)
          // then
          result.isInvalid shouldBe true
        }
      }
    }
  }

  private def stockPurchase = stockPurchaseBase(validComment)

  private def stockPurchaseBase: String => XTBReportRow = rowRepresentation(stocksEtfPurchase)

  private def deposit = rowRepresentation("Deposit")(validComment)

  private def withdrawal = rowRepresentation("Withdrawal")(validComment)

  private def stockSale = stockSaleBase(validComment)

  private def stockSaleBase: String => XTBReportRow = rowRepresentation(stocksEtfSale)

  private def dividend = rowRepresentation("Dividend")(validComment)

  private def witholdTax = rowRepresentation("Withholding tax")(validComment)

  private def rowRepresentation(operation: String)(comment: String) = {
    XTBReportRow(
      id,
      operation,
      dateTime,
      instrumentSymbol,
      comment,
      amount
    )
  }
}
