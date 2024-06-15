package com.zygiert

import cats.effect._
import cats.effect.kernel.Resource
import com.comcast.ip4s._
import com.zygiert.TaxCalculator.Environments.ImporterEnvironment
import com.zygiert.persistence.{EventRepository, LiveEventRepository}
import importer.ImporterRoutes
import mongo4cats.client.MongoClient
import org.http4s.dsl.io._
import org.http4s.ember.server._
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object TaxCalculator extends IOApp {

  object Environments {
    type ImporterEnvironment[F[_]] = EventRepository[F]
  }

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run(args: List[String]): IO[ExitCode] = {

    val mongoClient: Resource[IO, MongoClient[IO]] = MongoClient.fromConnectionString[IO]("mongodb://172.17.0.2:27017")
//    val clientFromServerAddress = MongoClient.fromServerAddress[IO](ServerAddress("172.17.0.2", 27017))
    val liveEnvironment: ImporterEnvironment[IO] = new LiveEventRepository[IO](mongoClient)

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(new ImporterRoutes[IO].importerRoutes(liveEnvironment).orNotFound)
      .withErrorHandler {
        case t: Throwable => BadRequest(t.getMessage)
      }
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

}
