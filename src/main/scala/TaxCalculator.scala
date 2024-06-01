import cats.effect._
import com.comcast.ip4s._
import importer.Routes
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object TaxCalculator extends IOApp {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(Routes.importerRoutes.orNotFound)
      .withErrorHandler {
        case t: Throwable => Ok(t.getMessage)
      }
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

}
