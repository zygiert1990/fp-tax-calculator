package com.zygiert.importer

import cats.data.{NonEmptyChain, ValidatedNec}
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
        resolveSingleCurrencyBroker(importRequest.broker)
          .fold(
            errorMessage => Left(errorMessage),
            (importer: SingleCurrency[RowRepresentation]) => doImport(importRequest)(importer)(rows => importer.toEvents(rows, importRequest.broker, value)))
      case None =>
        resolveMultipleCurrencyBroker(importRequest.broker)
          .fold(
            errorMessage => Left(errorMessage),
            (importer: MultipleCurrency[RowRepresentation]) => doImport(importRequest)(importer)(rows => importer.toEvents(rows, importRequest.broker)))
    }
  }

  private def doImport[F[_] : Concurrent, T <: RowRepresentation](importRequest: ImportRequest[F])
                                                                 (importer: ReportImporter[T])
                                                                 (rowsMappingFunction: List[T] => ValidatedNec[String, List[Event]]): Either[String, F[Unit]] = {
    importRequest.report.decodeString(importer.charset)
      .fold(
        error => Left(error.getMessage),
        fileContent =>
          validateHeader(fileContent)(importer)
            .flatMap(content => toRowRepresentations(content)(importer).toEither)
            .flatMap(rows => rowsMappingFunction(rows).toEither)
            .fold(
              errorChain => Left(errorChain.mkString_("\n")),
              events => Right(importRequest.env.eventRepository.saveAll(events))
            )
      )
  }

  private def validateHeader[T <: RowRepresentation, F[_] : Concurrent](fileContent: String)(importer: ReportImporter[T]): Either[NonEmptyChain[String], String] = {
    Either.cond(
      fileContent.lines().toScala(LazyList).take(1).exists(header => header.equals(importer.validHeader)),
      fileContent,
      NonEmptyChain(s"Invalid headers! Header should look like ${importer.validHeader}"))
  }

  private def resolveSingleCurrencyBroker[T <: RowRepresentation](broker: Broker): Either[String, SingleCurrency[T]] = {
    broker.symbol match {
      case "XTB" => Right(XTBImporter.asInstanceOf[SingleCurrency[T]])
      case _ => Left(s"Can not find single currency importer implementation for broker: ${broker.symbol}")
    }
  }

  private def resolveMultipleCurrencyBroker[T <: RowRepresentation](broker: Broker): Either[String, MultipleCurrency[T]] = {
    broker.symbol match {
      case "Exante" => Right(ExanteImporter.asInstanceOf[MultipleCurrency[T]])
      case _ => Left(s"Can not find multiple currency importer implementation for broker: ${broker.symbol}")
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
