package com.zygiert.importer

import cats.data.Validated.Valid
import com.zygiert.importer.ExanteImporter._
import com.zygiert.importer.Model.ExanteReportRow
import com.zygiert.model.Model.{Broker, Currency, Instrument}
import com.zygiert.persistence.Model.{DividendPaid, WithholdTaxCharged}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.time.LocalDateTime

class ExanteImporterTest extends AnyFunSpec with Matchers {

  private val broker = Broker("Exante")
  private val usdSymbol = "USD"
  private val sgdSymbol = "SGD"
  private val usdCurrency = Currency(usdSymbol)

  describe("ExanteImporter") {
    describe("mapping to report row representation") {
      describe("invalid cases") {
        it("should return invalid when cannot parse date time") {
          // given
          val row = "\"528655723\"\t\"ONM1018.001\"\t\"SILJ.ARCA\"\t\"None\"\t\"US TAX\"\t\"2023-12-29T06:04:15\"\t\"-0.01\"\t\"USD\"\t\"-0.01\"\t\"31 shares ExD 2023-12-27 PD 2023-12-29 dividend SILJ.ARCA 0.03 USD (0.0008575 per share) tax -0.01 USD (-15.000%) DivCntry US USIncmCode 52\"\t\"290b2c8c-2798-46e3-bb8a-c114764218ef\"\t\"None\""
          // when
          val result = toReportRowRepresentation(row)
          // then
          result.isInvalid shouldBe true
        }
        it("should return invalid when cannot parse value") {
          // given
          val row = "\"528655723\"\t\"ONM1018.001\"\t\"SILJ.ARCA\"\t\"None\"\t\"US TAX\"\t\"2023-12-29 06:04:15\"\t\"-0.01T\"\t\"USD\"\t\"-0.01\"\t\"31 shares ExD 2023-12-27 PD 2023-12-29 dividend SILJ.ARCA 0.03 USD (0.0008575 per share) tax -0.01 USD (-15.000%) DivCntry US USIncmCode 52\"\t\"290b2c8c-2798-46e3-bb8a-c114764218ef\"\t\"None\""
          // when
          val result = toReportRowRepresentation(row)
          // then
          result.isInvalid shouldBe true
        }
      }
      describe("valid cases") {
        it("should parse row with negative value") {
          // given
          val row = "\"528655723\"\t\"ONM1018.001\"\t\"SILJ.ARCA\"\t\"None\"\t\"US TAX\"\t\"2023-12-29 06:04:15\"\t\"-0.01\"\t\"USD\"\t\"-0.01\"\t\"31 shares ExD 2023-12-27 PD 2023-12-29 dividend SILJ.ARCA 0.03 USD (0.0008575 per share) tax -0.01 USD (-15.000%) DivCntry US USIncmCode 52\"\t\"290b2c8c-2798-46e3-bb8a-c114764218ef\"\t\"None\""
          // when
          val result = toReportRowRepresentation(row)
          // then
          result shouldEqual Valid(validRow)
        }
        it("should parse row with positive value") {
          // given
          val row = "\"528655723\"\t\"ONM1018.001\"\t\"SILJ.ARCA\"\t\"None\"\t\"US TAX\"\t\"2023-12-29 06:04:15\"\t\"0.01\"\t\"USD\"\t\"-0.01\"\t\"31 shares ExD 2023-12-27 PD 2023-12-29 dividend SILJ.ARCA 0.03 USD (0.0008575 per share) tax -0.01 USD (-15.000%) DivCntry US USIncmCode 52\"\t\"290b2c8c-2798-46e3-bb8a-c114764218ef\"\t\"None\""
          // when
          val result = toReportRowRepresentation(row)
          // then
          result shouldEqual Valid(validRow)
        }
      }
    }
    describe("mapping to events") {
      describe("successful") {
        it("should map US TAX and DIVIDEND to events") {
          // given
          val dateTime = LocalDateTime.of(2023, 12, 29, 6, 4, 15)
          val instrument = "SILJ.ARCA"
          val dividendAmount = BigDecimal(0.03)
          val taxAmount = BigDecimal(0.01)
          val rows = List(
            ExanteReportRow("528655723", instrument, "US TAX", dateTime, taxAmount, usdSymbol, "31 shares ExD 2023-12-27 PD 2023-12-29 dividend SILJ.ARCA 0.03 USD\n(0.0008575 per share) tax -0.01 USD (-15.000%) DivCntry US\nUSIncmCode 52"),
            ExanteReportRow("528655722", instrument, "DIVIDEND", dateTime, dividendAmount, usdSymbol, "31 shares ExD 2023-12-27 PD 2023-12-29 dividend SILJ.ARCA 0.03 USD\n(0.0008575 per share) tax -0.01 USD (-15.000%) DivCntry US\nUSIncmCode 52"))
          // when
          val result = toEvents(rows, broker)
          // when
          result shouldEqual Valid(
            List(WithholdTaxCharged(dateTime, broker, taxAmount, usdCurrency), DividendPaid(dateTime, broker, Instrument(instrument), dividendAmount, usdCurrency)))
        }
        it("should map DIVIDEND and AUTOCONVERSION's to events") {
          // given
          val dateTime = LocalDateTime.of(2023, 11, 2, 4, 1, 31)
          val instrument = "COI.SGX"
          val dividendSgdAmount = BigDecimal(5.2)
          val dividendUsdAmount = BigDecimal(3.8)
          val rows = List(
            ExanteReportRow("514216879", instrument, "DIVIDEND", dateTime, dividendSgdAmount, sgdSymbol, "2000 shares ExD 2023-10-02 PD 2023-11-02 dividend COI.SGX 5.20 SGD\n(0.002600 per share) tax -0.00 SGD (-0.0%) DivCntry SG"),
            ExanteReportRow("514216880", "None", "AUTOCONVERSION", dateTime, dividendSgdAmount, sgdSymbol, "crossrate=0.7327247399767 commission=0.002 conversion\nrate=0.7312592904967466 is commission applied=true"),
            ExanteReportRow("514216881", "None", "AUTOCONVERSION", dateTime, dividendUsdAmount, usdSymbol, "crossrate=0.7327247399767 commission=0.002 conversion\nrate=0.7312592904967466 is commission applied=true"))
          // when
          val result = toEvents(rows, broker)
          // when
          result shouldEqual Valid(
            List(DividendPaid(dateTime, broker, Instrument(instrument), dividendSgdAmount, usdCurrency)))
        }
      }
    }
  }

  private def validRow = {
    ExanteReportRow("528655723", "SILJ.ARCA", "US TAX", LocalDateTime.of(2023, 12, 29, 6, 4, 15), BigDecimal(0.01), "USD", "31 shares ExD 2023-12-27 PD 2023-12-29 dividend SILJ.ARCA 0.03 USD (0.0008575 per share) tax -0.01 USD (-15.000%) DivCntry US USIncmCode 52")
  }
}
