package com.zygiert.importer

import java.time.LocalDateTime

object Model {

  sealed trait RowRepresentation

  type ReportRowRepresentation <: RowRepresentation

  case class XTBReportRow(id: String,
                          operationType: String,
                          dateTime: LocalDateTime,
                          symbol: String,
                          comment: String,
                          amount: BigDecimal) extends RowRepresentation

  case class ExanteReportRow(transactionId: String) extends RowRepresentation

}
