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
import org.http4s.{Charset, EntityDecoder, HttpRoutes, MediaRange, QueryParamDecoder}
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

import java.io.InputStream

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
                fa => BadRequest(fa),
                fb => fb.flatMap { importer =>
                  req.as[ByteVector].flatMap { bv =>
                    val report = bv.decodeString(importer.charset)
                    report.fold(
                      fa => BadRequest(fa.getMessage),
                      fb => {
                        val rowRepresentations = fb.lines().toScala(LazyList)
                          .drop(1)
                          .filter(line => !line.isBlank)
                          .map(importer.toReportRowRepresentation)
                          .toList
                          .sequence
                        rowRepresentations match {
                          case Valid(rows) =>
                            val events = importer.toEvents(rows, currency)
                            Ok("anything")
                          case Invalid(nec) => BadRequest(nec.mkString_("\n"))
                        }
                      }
                    )
                  }
                }
              )
          case None => ???
        }
      /*req.as[ByteVector]
        .flatMap { bv =>
          broker.value match {
            case xtb@"XTB" =>
              val report = bv.decodeString(Charset.`UTF-8`.nioCharset)
              report.fold(
                fa => BadRequest(fa.toString),
                fb => {
                  val rows = fb.lines().toScala(LazyList)
                    .drop(1)
                    .filter(line => !line.isBlank)
                    .traverse(Importer.XTBImporter.toReportRowRepresentation)

                  rows match {
                    case Valid(rowRepresentations) =>
                      currency match {
                        case Some(symbol) =>
                          val events = XTBImporter.toEvents(rowRepresentations, symbol)
                          Ok("anyhting")
                        case None         => BadRequest(s"Currency has to be provided for $xtb broker")
                      }
                    case Invalid(nec)              => BadRequest(nec.mkString_("\n"))
                  }
                }
              )
            case "Exante" =>
              val report = bv.decodeString(Charset.`UTF-16`.nioCharset)
              report.fold(
                fa => BadRequest(fa.toString),
                fb => {
                  val rows = fb.lines().toScala(LazyList)
                    .drop(1)
                    .map(Importer.ExanteImporter.toReportRowRepresentation)
                    .toList
                  Ok("anyhting")
                }
              )
          }
        }*/
    }
  }

  def resolveSingleCurrencyImporter(broker: Broker): Either[String, IO[SingleCurrency[ReportRowRepresentation]]] = {
    broker.value match {
      case "XTB" => Right(IO(XTBImporter.asInstanceOf[SingleCurrency[ReportRowRepresentation]]))
      case _ => Left(s"Can not find single currency importer for broker: ${broker.value}")
    }
  }

  def resolveMultipleCurrencyImporter[T <: RowRepresentation](broker: Broker): Either[String, IO[MultipleCurrency[_]]] = {
    broker.value match {
      case "Exante" => Right(ExanteImporter.pure[IO])
      case _ => Left(s"Can not find single currency importer for broker: ${broker.value}")
    }
  }

}
