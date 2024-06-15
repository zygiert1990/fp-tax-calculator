package com.zygiert.importer

import cats.data.Validated.Valid
import com.zygiert.importer.Validations._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.time.LocalDateTime

class ValidationsTest extends AnyFunSpec with Matchers {

  private val format = "dd.MM.yyyy HH:mm:ss"

  describe("Validations") {
    describe("Parsing local date time") {
      it("should pass when valid string") {
        // given
        val value = "15.12.2022 16:55:21"
        // when
        val result = canParseToLocalDateTime(value, format)
        // then
        result shouldEqual Valid(LocalDateTime.of(2022, 12, 15, 16, 55, 21))
      }

      it("should fail when invalid string") {
        // given
        val value = "15.12.2022T16:55:21"
        // when
        val result = canParseToLocalDateTime(value, format)
        // then
        result.isInvalid shouldBe true
      }
    }

    describe("Parsing BigDecimal") {
      it("should pass when valid string") {
        // given
        val value = "100"
        // when
        val result = canParseToBigDecimal(value)
        // then
        result shouldEqual Valid(BigDecimal(100))
      }

      it("should fail when invalid string") {
        // given
        val value = "100f"
        // when
        val result = canParseToBigDecimal(value)
        // then
        result.isInvalid shouldBe true
      }
    }
  }

}
