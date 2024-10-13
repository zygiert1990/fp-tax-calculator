package com.zygiert.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

object Model {

  case class Currency(symbol: String)
  case class Instrument(symbol: String)
  case class Broker(symbol: String)

  object Currency {
    implicit val decodeEvent: Decoder[Currency] = deriveDecoder[Currency]
    implicit val encodeEvent: Encoder.AsObject[Currency] = deriveEncoder[Currency]
  }

  object Instrument {
    implicit val decodeEvent: Decoder[Instrument] = deriveDecoder[Instrument]
    implicit val encodeEvent: Encoder.AsObject[Instrument] = deriveEncoder[Instrument]
  }

  object Broker {
    implicit val decodeEvent: Decoder[Broker] = deriveDecoder[Broker]
    implicit val encodeEvent: Encoder.AsObject[Broker] = deriveEncoder[Broker]
  }

}
