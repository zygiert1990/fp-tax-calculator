package com.zygiert.importer

import java.time.LocalDateTime

object Model {

  sealed trait RowRepresentation

  type ReportRowRepresentation <: RowRepresentation

  case class XTBReportRow(id: String,
                          operationType: String,
                          dateTime: LocalDateTime,
                          instrumentSymbol: String,
                          comment: String,
                          amount: BigDecimal) extends RowRepresentation

  case class ExanteReportRow(id: String,
                             instrumentSymbol: String,
                             operationType: String,
                             dateTime: LocalDateTime,
                             value: BigDecimal,
                             asset: String,
                             comment: String) extends RowRepresentation

}
