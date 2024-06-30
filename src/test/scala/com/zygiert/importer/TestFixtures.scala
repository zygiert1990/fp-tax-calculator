package com.zygiert.importer

import cats.effect.IO
import com.zygiert.TaxCalculator.Environments.ImporterEnvironment
import com.zygiert.persistence.TestEventRepository
import scodec.bits.ByteVector

object TestFixtures {

  val testEnv: ImporterEnvironment[IO] = new TestEventRepository

  def testReport(filePath: String): ByteVector = ByteVector(getClass.getResourceAsStream(filePath).readAllBytes())

}
