package com.zygiert.config

import com.typesafe.scalalogging.StrictLogging
import pureconfig._
import pureconfig.generic.semiauto._

case class MongoConfig(host: String, port: Int)

case class Config(mongo: MongoConfig)


object Config extends StrictLogging {
  def log(config: Config): Unit = {
    val baseInfo =
      s"""
         |TaxCalculator configuration:
         |-----------------------
         |MONGO:             ${config.mongo}
         |
         |""".stripMargin

    logger.info(baseInfo)
  }

  implicit val configReader: ConfigReader[Config] = deriveReader[Config]

  def read: Config = ConfigSource.default.loadOrThrow[Config]
}
