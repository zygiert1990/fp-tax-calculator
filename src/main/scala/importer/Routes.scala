package importer

import cats.effect._
import importer.ImporterDefinition.{ExanteImporter, MultipleCurrency, SingleCurrency}
import importer.Model.ReportRowRepresentation
import model.Model.{Broker, Currency}
import org.http4s.dsl.io._
import org.http4s.{HttpRoutes, QueryParamDecoder}
import scodec.bits.ByteVector

object Routes {

  implicit val brokerQueryParamMatcher: QueryParamDecoder[Broker] = QueryParamDecoder[String].map(Broker)
  implicit val optionalCurrencyQueryParamMatcher: QueryParamDecoder[Currency] = QueryParamDecoder[String].map(Currency)

  object BrokerQueryParamMatcher extends QueryParamDecoderMatcher[Broker]("broker")

  object OptionalCurrencyQueryParamDecoder extends OptionalQueryParamDecoderMatcher[Currency]("currency")

  def importerRoutes: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req@POST -> Root / "import" :? BrokerQueryParamMatcher(broker) +& OptionalCurrencyQueryParamDecoder(currency) =>
        currency match {
          case Some(currency) =>
            resolveSingleCurrencyImporter(broker)
              .fold(
                error => BadRequest(error),
                importer => req.as[ByteVector].flatMap(request => Importer.process(request, importer)(rows => importer.toEvents(rows, currency)))
              )
          case None =>
            resolveMultipleCurrencyImporter(broker)
              .fold(
                error => BadRequest(error),
                importer => req.as[ByteVector].flatMap(request => Importer.process(request, importer)(importer.toEvents))
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
