package com.zygiert.importer

import cats.data.ValidatedNec
import cats.effect.kernel.Concurrent
import cats.implicits._
import com.zygiert.TaxCalculator.Environments.ImporterEnvironment
import com.zygiert.importer.ImporterDefinition.{MultipleCurrency, ReportImporter, SingleCurrency}
import com.zygiert.importer.Model.RowRepresentation
import com.zygiert.model.Model.{Broker, Currency}
import com.zygiert.persistence.Model.Event
import scodec.bits.ByteVector

import scala.jdk.StreamConverters._

object ImportHandler {

  case class ImportRequest[F[_] : Concurrent](env: ImporterEnvironment[F], broker: Broker, optionalCurrency: Option[Currency], report: ByteVector)

  def handleImport[F[_] : Concurrent](importRequest: ImportRequest[F]): Either[String, F[Unit]] = {
    importRequest.optionalCurrency match {
      case Some(value) =>
        val importer = resolveSingleCurrencyBroker(importRequest.broker)
        doImport(importRequest)(importer)(rows => importer.toEvents(rows, importRequest.broker, value))
      case None =>
        val importer = resolveMultipleCurrencyBroker(importRequest.broker)
        doImport(importRequest)(importer)(rows => importer.toEvents(rows, importRequest.broker))
    }
  }

  private def doImport[F[_] : Concurrent, T <: RowRepresentation](importRequest: ImportRequest[F])
                                                                 (importer: ReportImporter[T])
                                                                 (rowsMappingFunction: List[T] => ValidatedNec[String, List[Event]]): Either[String, F[Unit]] = {
    importRequest.report.decodeString(importer.charset)
      .fold(
        error => Left(error.getMessage),
        fileContent => toRowRepresentations(fileContent)(importer).toEither.flatMap(rows => rowsMappingFunction(rows).toEither)
          .fold(
            errorChain => Left(errorChain.mkString_("\n")),
            events => Right(importRequest.env.eventRepository.saveAll(events))
          )
      )
  }

  private def resolveSingleCurrencyBroker[T <: RowRepresentation](broker: Broker): SingleCurrency[T] = {
    broker.symbol match {
      case "XTB" => XTBImporter.asInstanceOf[SingleCurrency[T]]
      case _ => throw new IllegalArgumentException(s"Can not find single currency importer implementation for broker: ${broker.symbol}")
    }
  }

  private def resolveMultipleCurrencyBroker[T <: RowRepresentation](broker: Broker): MultipleCurrency[T] = {
    broker.symbol match {
      case _ => throw new IllegalArgumentException(s"Can not find multiple currency importer implementation for broker: ${broker.symbol}")
    }
  }

  private def toRowRepresentations[T <: RowRepresentation](fileContent: String)(importer: ReportImporter[T]): ValidatedNec[String, List[T]] = {
    fileContent.lines().toScala(LazyList)
      .drop(1)
      .filter(!_.isBlank)
      .map(importer.toReportRowRepresentation)
      .toList
      .sequence
  }
}
