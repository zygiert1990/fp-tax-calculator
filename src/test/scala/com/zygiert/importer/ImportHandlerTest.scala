package com.zygiert.importer

import cats.effect.IO
import com.zygiert.TaxCalculator.Environments.ImporterEnvironment
import com.zygiert.model.Model.{Broker, Currency}
import com.zygiert.persistence.EventRepository
import com.zygiert.persistence.Model._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import scodec.bits.ByteVector

class ImportHandlerTest extends AnyFunSpec with Matchers {

  private val testEnv: ImporterEnvironment[IO] = new TestEventRepository
  private val currencyOption = Some(Currency("USD"))
  private val xtbValidReportPath = "/test-importer/validXTB.csv"
  private val xtbInvalidRowsReportPath = "/test-importer/invalidRowsXTB.csv"
  private val xtbInvalidEventsReportPath = "/test-importer/invalidEventsXTB.csv"
  private val xtb = Broker("XTB")

  describe("ImportHandler") {
    describe("with currency") {
      describe("failed") {
        it("should fail when can not find implementation for requested broker") {
          // given
          val request = importRequest(Broker("nonExisting"), xtbValidReportPath)
          // when
          val result = ImportHandler.handleImport(request)
          // then
          result.isLeft shouldBe true
        }

        it("should fail when can not transform rows from report") {
          // given
          val request = importRequest(xtb, xtbInvalidRowsReportPath)
          // when
          val result = ImportHandler.handleImport(request)
          // then
          result.isLeft shouldBe true
        }

        it("should fail when can not create events from report") {
          // given
          val request = importRequest(xtb, xtbInvalidEventsReportPath)
          // when
          val result = ImportHandler.handleImport(request)
          // then
          result.isLeft shouldBe true
        }
      }
      describe("success") {
        it("should import report") {
          // given
          val request = importRequest(xtb, xtbValidReportPath)
          // when
          val result = ImportHandler.handleImport(request)
          // then
          result.isRight shouldBe true
        }
      }
    }
  }

  private def importRequest(broker: Broker, reportPath: String, currencyOption: Option[Currency] = currencyOption): ImportHandler.ImportRequest[IO] =
    ImportHandler.ImportRequest[IO](testEnv, broker, currencyOption, testReport(reportPath))

  private def testReport(filePath: String): ByteVector = ByteVector(getClass.getResourceAsStream(filePath).readAllBytes())

}

class TestEventRepository extends EventRepository[IO] {

  override val eventRepository: Repository = new Repository {
    override def saveAll(events: List[Event]): IO[Unit] = IO.println {
      s"Successfully stored: ${events.size} events"
    }

    override def findAll: IO[List[Event]] = ???
  }
}