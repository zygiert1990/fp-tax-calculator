package com.zygiert.importer

import cats.effect._
import cats.effect.unsafe.IORuntime
import com.zygiert.importer.TestFixtures.testReport
import com.zygiert.persistence.TestEventRepository
import fs2.Pure
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ImporterRoutesTest extends AnyFunSpec with Matchers {

  implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global
  private val routes: ImporterRoutes = ImporterRoutes(ImportHandler(TestEventRepository()))
  private val xtbValidReportPath = "/test-importer/validXTB.csv"
  private val xtbInvalidRowsReportPath = "/test-importer/invalidRowsXTB.csv"
  private val expectedErrorMessage = "Can not parse 15.12.2022 16:55:AA to LocalDateTime due to: Text '15.12.2022 16:55:AA' could not be parsed at index 17\nCan not parse -0.02, to BigDecimal due to: Character , is neither a decimal digit number, decimal point, nor \"e\" notation exponential mark."

  describe("ImporterRoutes") {
    it("should return status 200 when import was successful") {
      // given
      val request = prepareRequest(xtbValidReportPath)
      // when
      val result: IO[Response[IO]] = performRequest(request)
      // then
      check[String](result, Status.Ok, Some("Import successful!")) shouldBe true
    }

    it("should return status 400 when import failed") {
      // given
      val request = prepareRequest(xtbInvalidRowsReportPath)
      // when
      val result: IO[Response[IO]] = performRequest(request)
      // then
      check[String](result, Status.BadRequest, Some(expectedErrorMessage)) shouldBe true
    }
  }

  private def prepareRequest(report: String): Request[Pure] =
    Request(method = POST, uri = uri"/import?broker=XTB&currency=USD", entity = Entity.strict(testReport(report)))

  private def performRequest(request: Request[Pure]): IO[Response[IO]] = {
    routes.routes().orNotFound.run(request)
  }

  private def check[A](actual: IO[Response[IO]], expectedStatus: Status, expectedBody: Option[A])(implicit ev: EntityDecoder[IO, A]): Boolean = {
    val actualResp = actual.unsafeRunSync()
    val statusCheck = actualResp.status == expectedStatus
    val bodyCheck = expectedBody.fold[Boolean](
      // Verify Response's body is empty.
      actualResp.body.compile.toVector.unsafeRunSync().isEmpty)(
      expected => actualResp.as[A].unsafeRunSync() == expected
    )
    statusCheck && bodyCheck
  }

}
