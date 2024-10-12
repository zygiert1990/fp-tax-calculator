package com.zygiert.importer

import com.zygiert.importer.TestFixtures.testReport
import com.zygiert.model.Model.{Broker, Currency}
import com.zygiert.persistence.TestEventRepository
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ImportHandlerTest extends AnyFunSpec with Matchers {

  private val currencyOption = Some(Currency("USD"))
  private val xtbValidReportPath = "/test-importer/validXTB.csv"
  private val xtbInvalidHeaderPath = "/test-importer/invalidHeaderXTB.csv"
  private val xtbInvalidRowsReportPath = "/test-importer/invalidRowsXTB.csv"
  private val xtbInvalidEventsReportPath = "/test-importer/invalidEventsXTB.csv"
  private val exanteValidReportPath = "/test-importer/validExante.csv"
  private val exanteInvalidHeaderPath = "/test-importer/invalidHeaderExante.csv"
  private val exanteInvalidRowsReportPath = "/test-importer/invalidRowsExante.csv"
  private val exanteInvalidEventsReportPath = "/test-importer/invalidEventsExante.csv"
  private val xtb = Broker("XTB")
  private val exante = Broker("Exante")
  private val importHandler = ImportHandler(TestEventRepository())

  describe("ImportHandler") {
    describe("with currency") {
      describe("failed") {
        it("should fail when can not find implementation for requested broker") {
          // given
          val request = importRequest(Broker("nonExisting"), xtbValidReportPath)
          // when
          val result = importHandler.handleImport(request)
          // then
          result.isLeft shouldBe true
        }
        it("should fail when invalid header") {
          // given
          val request = importRequest(xtb, xtbInvalidHeaderPath)
          // when
          val result = importHandler.handleImport(request)
          // then
          result.isLeft shouldBe true
        }
        it("should fail when can not transform rows from report") {
          // given
          val request = importRequest(xtb, xtbInvalidRowsReportPath)
          // when
          val result = importHandler.handleImport(request)
          // then
          result.isLeft shouldBe true
        }
        it("should fail when can not create events from report") {
          // given
          val request = importRequest(xtb, xtbInvalidEventsReportPath)
          // when
          val result = importHandler.handleImport(request)
          // then
          result.isLeft shouldBe true
        }
      }
      describe("success") {
        it("should import report") {
          // given
          val request = importRequest(xtb, xtbValidReportPath)
          // when
          val result = importHandler.handleImport(request)
          // then
          result.isRight shouldBe true
        }
      }
    }
    describe("without currency") {
      describe("failed") {
        it("should fail when can not find implementation for requested broker") {
          // given
          val request = importRequest(Broker("nonExisting"), exanteValidReportPath, None)
          // when
          val result = importHandler.handleImport(request)
          // then
          result.isLeft shouldBe true
        }
        it("should fail when invalid header") {
          // given
          val request = importRequest(exante, exanteInvalidHeaderPath, None)
          // when
          val result = importHandler.handleImport(request)
          // then
          result.isLeft shouldBe true
        }
        it("should fail when can not transform rows from report") {
          // given
          val request = importRequest(exante, exanteInvalidRowsReportPath, None)
          // when
          val result = importHandler.handleImport(request)
          // then
          result.isLeft shouldBe true
        }

        // todo not implemented yet
        //        it("should fail when can not create events from report") {
        //          // given
        //          val request = importRequest(xtb, xtbInvalidEventsReportPath)
        //          // when
        //          val result = importHandler.handleImport(request)
        //          // then
        //          result.isLeft shouldBe true
        //        }
      }
      // todo not implemented yet
      //      describe("success") {
      //        it("should import report") {
      //          // given
      //          val request = importRequest(xtb, xtbValidReportPath)
      //          // when
      //          val result = importHandler.handleImport(request)
      //          // then
      //          result.isRight shouldBe true
      //        }
      //      }
    }
  }

  private def importRequest(broker: Broker, reportPath: String, currencyOption: Option[Currency] = currencyOption): ImportRequest =
    ImportRequest(broker, currencyOption, testReport(reportPath))

}