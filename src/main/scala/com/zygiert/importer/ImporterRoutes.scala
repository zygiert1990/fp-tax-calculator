package com.zygiert.importer

import cats.effect._
import com.zygiert.model.Model.{Broker, Currency}
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, QueryParamDecoder}
import scodec.bits.ByteVector

object ImporterRoutes {
  def apply(importHandler: ImportHandler): ImporterRoutes = {
    new ImporterRoutes(importHandler)
  }
}

class ImporterRoutes private(private val importHandler: ImportHandler) extends Http4sDsl[IO] {

  implicit val optionalCurrencyQueryParamMatcher: QueryParamDecoder[Currency] = QueryParamDecoder[String].map(Currency.apply)
  implicit val brokerQueryParamMatcher: QueryParamDecoder[Broker] = QueryParamDecoder[String].map(Broker.apply)

  private object BrokerQueryParamMatcher extends QueryParamDecoderMatcher[Broker]("broker")

  private object OptionalCurrencyQueryParamDecoder extends OptionalQueryParamDecoderMatcher[Currency]("currency")

  def routes(): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req@POST -> Root / "import" :? BrokerQueryParamMatcher(broker) +& OptionalCurrencyQueryParamDecoder(optionalCurrency) =>
        req.as[ByteVector].flatMap { report =>
          importHandler.handleImport(ImportRequest(broker, optionalCurrency, report))
            .fold(errorMessage => BadRequest(errorMessage), _ => Ok("Import successful!"))
        }
    }
  }

}
