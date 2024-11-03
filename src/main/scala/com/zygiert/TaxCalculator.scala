package com.zygiert

import cats.effect.*
import com.comcast.ip4s._
import com.zygiert.config.Config
import com.zygiert.importer.{ImportHandler, ImporterRoutes}
import com.zygiert.persistence.EventRepositoryImpl
import mongo4cats.client.MongoClient
import mongo4cats.models.client.ServerAddress
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object TaxCalculator extends IOApp {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run(args: List[String]): IO[ExitCode] = {

    val config: Config = Config.read
    Config.log(config)

    val mongoClient = MongoClient.fromServerAddress[IO](ServerAddress(config.mongo.host, config.mongo.port))
    EmberServerBuilder
      .default[IO]
      .withHost(host"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(ImporterRoutes(ImportHandler(EventRepositoryImpl(mongoClient))).routes().orNotFound)
      .withErrorHandler {
        case t: Throwable => BadRequest(t.getMessage)
      }
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

}
