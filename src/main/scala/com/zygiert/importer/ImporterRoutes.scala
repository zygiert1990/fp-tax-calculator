package com.zygiert.importer

import cats.effect._
import cats.implicits._
import com.zygiert.TaxCalculator.Environments.ImporterEnvironment
import com.zygiert.model.Model.{Broker, Currency}
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, QueryParamDecoder}
import scodec.bits.ByteVector

class ImporterRoutes[F[_] : Concurrent] extends Http4sDsl[F] {

  implicit val optionalCurrencyQueryParamMatcher: QueryParamDecoder[Currency] = QueryParamDecoder[String].map(Currency)
  implicit val brokerQueryParamMatcher: QueryParamDecoder[Broker] = QueryParamDecoder[String].map(Broker)

  object BrokerQueryParamMatcher extends QueryParamDecoderMatcher[Broker]("broker")

  object OptionalCurrencyQueryParamDecoder extends OptionalQueryParamDecoderMatcher[Currency]("currency")

  def routes(env: ImporterEnvironment[F]): HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case req@POST -> Root / "import" :? BrokerQueryParamMatcher(broker) +& OptionalCurrencyQueryParamDecoder(optionalCurrency) =>
        req.as[ByteVector].flatMap { report =>
          ImportHandler.handleImport(ImportHandler.ImportRequest(env, broker, optionalCurrency, report))
            .fold(errorMessage => BadRequest(errorMessage), _ => Ok("Import successful!"))
        }
    }
  }

}
