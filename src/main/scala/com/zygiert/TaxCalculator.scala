package com.zygiert

import cats.effect._
import com.comcast.ip4s._
import com.zygiert.TaxCalculator.Environments.ImporterEnvironment
import com.zygiert.config.Config
import com.zygiert.importer.ImporterRoutes
import com.zygiert.persistence.{EventRepository, LiveEventRepository}
import mongo4cats.client.MongoClient
import mongo4cats.models.client.ServerAddress
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

    val config: Config = Config.read
    Config.log(config)

    val mongoClient = MongoClient.fromServerAddress[IO](ServerAddress(config.mongo.host, config.mongo.port))
    val importerEnvironment: ImporterEnvironment[IO] = new LiveEventRepository[IO](mongoClient)

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(new ImporterRoutes[IO].routes(importerEnvironment).orNotFound)
      .withErrorHandler {
        case t: Throwable => BadRequest(t.getMessage)
      }
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

}
