package com.zygiert.importer

import cats.data.{NonEmptyChain, Validated, ValidatedNec}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

object Validations {

  def canParseToLocalDateTime(value: String, format: String): ValidatedNec[String, LocalDateTime] = {
    Validated.fromTry[LocalDateTime](Try(LocalDateTime.parse(value, DateTimeFormatter.ofPattern(format))))
      .leftMap(t => NonEmptyChain(s"Can not parse $value to LocalDateTime due to: ${t.getMessage}"))
  }

  def canParseToBigDecimal(value: String): ValidatedNec[String, BigDecimal] = {
    Validated.fromTry[BigDecimal](Try(BigDecimal(value)))
      .leftMap(t => NonEmptyChain(s"Can not parse $value to BigDecimal due to: ${t.getMessage}"))
  }

}
