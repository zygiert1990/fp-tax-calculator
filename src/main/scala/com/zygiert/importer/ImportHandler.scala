package com.zygiert.importer

import cats.data.ValidatedNec
import cats.effect.kernel.Concurrent
import cats.implicits._
import com.zygiert.TaxCalculator.Environments.ImporterEnvironment
import com.zygiert.importer.ImporterDefinition.{MultipleCurrency, ReportImporter, SingleCurrency}
import com.zygiert.importer.Model.RowRepresentation
import com.zygiert.model.Model.{Broker, Currency}
import com.zygiert.persistence.Model.Event
import org.http4s.Request
import scodec.bits.ByteVector

import java.nio.charset.CharacterCodingException
import scala.jdk.StreamConverters._

object ImportHandler {

  def handleImport[F[_] : Concurrent](env: ImporterEnvironment[F], broker: Broker, optionalCurrency: Option[Currency], req: Request[F]): F[Either[String, F[Unit]]] = {
    optionalCurrency match {
      case Some(value) =>
        req.as[ByteVector]
          .map { request =>
            val importer = resolveSingleCurrencyBroker(broker)
            decodeReport(request)(importer)
              .fold(
                error => Left(error.getMessage),
                fileContent => toRowRepresentations(fileContent)(importer).toEither.flatMap(rows => toEvents(rows, broker, value)(importer).toEither)
                  .fold(
                    errorChain => Left(errorChain.mkString_("\n")),
                    events => Right(env.eventRepository.saveAll(events))
                  )
              )
          }
      case None =>
        req.as[ByteVector]
          .map { request =>
            val importer = resolveMultipleCurrencyBroker(broker)
            decodeReport(request)(importer)
              .fold(
                error => Left(error.getMessage),
                fileContent => toRowRepresentations(fileContent)(importer).toEither.flatMap(rows => toEvents(rows, broker)(importer).toEither)
                  .fold(
                    errorChain => Left(errorChain.mkString_("\n")),
                    events => Right(env.eventRepository.saveAll(events))
                  )
              )
          }
    }
  }

  private def resolveSingleCurrencyBroker[T<:RowRepresentation](broker: Broker): SingleCurrency[T] = {
    broker.symbol match {
      case "XTB" => XTBImporter.asInstanceOf[SingleCurrency[T]]
      case _     => throw new IllegalArgumentException(s"Can not find single currency importer implementation for broker: ${broker.symbol}")
    }
  }

  private def resolveMultipleCurrencyBroker[T<:RowRepresentation](broker: Broker): MultipleCurrency[T] = {
    broker.symbol match {
      case _     => throw new IllegalArgumentException(s"Can not find multiple currency importer implementation for broker: ${broker.symbol}")
    }
  }

  private def decodeReport[T<:RowRepresentation](report: ByteVector)(importer: ReportImporter[T]): Either[CharacterCodingException, String] =
    report.decodeString(importer.charset)

  private def toRowRepresentations[T<:RowRepresentation](fileContent: String)(importer: ReportImporter[T]): ValidatedNec[String, List[T]] = {
    fileContent.lines().toScala(LazyList)
      .drop(1)
      .filter(!_.isBlank)
      .map(importer.toReportRowRepresentation)
      .toList
      .sequence
  }

  private def toEvents[T<:RowRepresentation](rows: List[T], broker: Broker, currency: Currency)(importer: SingleCurrency[T]): ValidatedNec[String, List[Event]] =
    importer.toEvents(rows, broker, currency)

  private def toEvents[T<:RowRepresentation](rows: List[T], broker: Broker)(importer: MultipleCurrency[T]): ValidatedNec[String, List[Event]] =
    importer.toEvents(rows, broker)

}
