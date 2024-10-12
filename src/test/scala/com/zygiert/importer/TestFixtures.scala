package com.zygiert.importer

import scodec.bits.ByteVector

object TestFixtures {

  def testReport(filePath: String): ByteVector = ByteVector(getClass.getResourceAsStream(filePath).readAllBytes())

}
