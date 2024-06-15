package com.zygiert.importer

import cats.effect._
import cats.implicits._
import com.zygiert.TaxCalculator.Environments.ImporterEnvironment
import com.zygiert.importer.ImporterDefinition.{ExanteImporter, MultipleCurrency, SingleCurrency}
import com.zygiert.importer.Model.ReportRowRepresentation
import com.zygiert.model.Model.{Broker, Currency}
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, QueryParamDecoder}
import scodec.bits.ByteVector

class ImporterRoutes[F[_]: Concurrent] extends Http4sDsl[F] {

  implicit val brokerQueryParamMatcher: QueryParamDecoder[Broker] = QueryParamDecoder[String].map(Broker)
  implicit val optionalCurrencyQueryParamMatcher: QueryParamDecoder[Currency] = QueryParamDecoder[String].map(Currency)

  object BrokerQueryParamMatcher extends QueryParamDecoderMatcher[Broker]("broker")

  object OptionalCurrencyQueryParamDecoder extends OptionalQueryParamDecoderMatcher[Currency]("currency")

  def importerRoutes(env: ImporterEnvironment[F]): HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case req@POST -> Root / "import" :? BrokerQueryParamMatcher(broker) +& OptionalCurrencyQueryParamDecoder(currency) =>
        currency match {
          case Some(currency) =>
            resolveSingleCurrencyImporter(broker)
              .fold(
                error => BadRequest(error),
                importer => req.as[ByteVector]
                  .flatMap { request =>
                    request.decodeString(importer.charset)
                      .fold(
                        error => BadRequest(error.getMessage),
                        fileContent => ReportTransformer.process(fileContent, importer)(rows => importer.toEvents(rows, currency))
                          .fold(
                            errorChain => BadRequest(errorChain.mkString_("\n")),
                            events => env.eventRepository.saveAll(events).flatMap(_ => Ok("Import successful"))
                          )
                      )
                  }
              )
          case None =>
            resolveMultipleCurrencyImporter(broker)
              .fold(
                error => BadRequest(error),
                importer => req.as[ByteVector]
                  .flatMap { request =>
                    request.decodeString(importer.charset)
                      .fold(
                        error => BadRequest(error.getMessage),
                        fileContent => ReportTransformer.process(fileContent, importer)(rows => importer.toEvents(rows))
                          .fold(
                            errorChain => BadRequest(errorChain.mkString_("\n")),
                            events => env.eventRepository.saveAll(events).flatMap(_ => Ok("Import successful"))
                          )
                      )
                  }
              )
        }
    }
  }

  private def resolveSingleCurrencyImporter(broker: Broker): Either[String, SingleCurrency[ReportRowRepresentation]] = {
    broker.value match {
      case "XTB" => Right(XTBImporter.asInstanceOf[SingleCurrency[ReportRowRepresentation]])
      case _ => Left(s"Can not find single currency importer for broker: ${broker.value}")
    }
  }

  private def resolveMultipleCurrencyImporter(broker: Broker): Either[String, MultipleCurrency[ReportRowRepresentation]] = {
    broker.value match {
      case "Exante" => Right(ExanteImporter.asInstanceOf[MultipleCurrency[ReportRowRepresentation]])
      case _ => Left(s"Can not find single currency importer for broker: ${broker.value}")
    }
  }

}
