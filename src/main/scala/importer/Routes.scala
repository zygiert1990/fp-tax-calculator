package importer


import cats.data.{ReaderT, ValidatedNec}
import cats.data.Validated.{Invalid, Valid}
import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import importer.Importer.{ExanteImporter, MultipleCurrency, SingleCurrency}
import importer.Routes
import org.http4s.Charset.`UTF-8`
import org.http4s.EntityDecoder.collectBinary
import org.http4s.{Charset, EntityDecoder, HttpRoutes, MediaRange, QueryParamDecoder, Response}
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import scodec.bits.ByteVector

import scala.jdk.StreamConverters._
import cats.implicits._
import importer.Model.{ReportRowRepresentation, RowRepresentation}
import model.Model.{Broker, Currency}
import persistence.DbClient
import persistence.Model.Event
import mongo4cats.circe._
import io.circe.generic.auto._

import java.io.InputStream
import java.nio.charset.CharacterCodingException

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
                importer => {
                  for {
                    request   <- req.as[ByteVector]
                    report    = request.decodeString(importer.charset)
                    res       <- process(report, importer, currency)
                  } yield res
                }
              )
          case None => BadRequest("asldljaskdl")
        }
    }
  }

  private def process(either: Either[CharacterCodingException, String], importer: SingleCurrency[ReportRowRepresentation], currency: Currency): IO[Response[IO]] = {
    either.fold(
      error       => BadRequest(error.getMessage),
      fileContent => {
        val validatedRowRepresentations = fileContent.lines().toScala(LazyList)
          .drop(1)
          .filter(line => !line.isBlank)
          .map(importer.toReportRowRepresentation)
          .toList
          .sequence

        validatedRowRepresentations match {
          case Valid(rr)  =>
            val validatedEvents = importer.toEvents(rr, currency)
            validatedEvents match {
              case Valid(events) => DbClient.saveAll(events).flatMap(_ => Ok("any"))
              case Invalid(e)    => BadRequest(e.mkString_("\n"))
            }
          case Invalid(e) => BadRequest(e.mkString_("\n"))
        }
      }
    )
  }

  private def resolveSingleCurrencyImporter(broker: Broker): Either[String, SingleCurrency[ReportRowRepresentation]] = {
    broker.value match {
      case "XTB" => Right(XTBImporter.asInstanceOf[SingleCurrency[ReportRowRepresentation]])
      case _ => Left(s"Can not find single currency importer for broker: ${broker.value}")
    }
  }

  private def resolveMultipleCurrencyImporter[T <: RowRepresentation](broker: Broker): Either[String, IO[MultipleCurrency[_]]] = {
    broker.value match {
      case "Exante" => Right(ExanteImporter.pure[IO])
      case _ => Left(s"Can not find single currency importer for broker: ${broker.value}")
    }
  }

}
