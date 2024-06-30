package com.zygiert.config

import com.typesafe.scalalogging.StrictLogging
import pureconfig.ConfigSource
import pureconfig.generic.auto._

case class MongoConfig(host: String, port: Int)
case class Config(mongo: MongoConfig)

object Config extends StrictLogging {
  def log(config: Config): Unit = {
    val baseInfo = s"""
                      |TaxCalculator configuration:
                      |-----------------------
                      |MONGO:             ${config.mongo}
                      |
                      |""".stripMargin

    logger.info(baseInfo)
  }

  def read: Config = ConfigSource.default.loadOrThrow[Config]
}
