package com.zygiert.importer

import cats.data.Validated.Valid
import com.zygiert.importer.ExanteImporter._
import com.zygiert.importer.Model.ExanteReportRow
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.time.LocalDateTime

class ExanteImporterTest extends AnyFunSpec with Matchers {

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
  }

  private def validRow = {
    ExanteReportRow("528655723", "SILJ.ARCA", "US TAX", LocalDateTime.of(2023, 12, 29, 6, 4, 15), BigDecimal(0.01), "USD", "31 shares ExD 2023-12-27 PD 2023-12-29 dividend SILJ.ARCA 0.03 USD (0.0008575 per share) tax -0.01 USD (-15.000%) DivCntry US USIncmCode 52")
  }
}
