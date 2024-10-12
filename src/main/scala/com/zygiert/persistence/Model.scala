package com.zygiert.persistence

import com.zygiert.model.Model.{Broker, Currency, Instrument}

import java.time.LocalDateTime

object Model {

  /*
    Please keep in mind that I'm not going to implement event sourcing here.
   */
  sealed trait Event

  final case class DepositMade(dateTime: LocalDateTime, broker: Broker, value: BigDecimal, currency: Currency) extends Event

  final case class WithdrawalDone(dateTime: LocalDateTime, broker: Broker, value: BigDecimal, currency: Currency) extends Event

  final case class AssetBought(dateTime: LocalDateTime, broker: Broker, instrument: Instrument, amount: Int,
                               totalPrice: BigDecimal, totalCommission: BigDecimal, currency: Currency) extends Event

  final case class AssetSold(dateTime: LocalDateTime, broker: Broker, instrument: Instrument, amount: Int,
                             totalPrice: BigDecimal, totalCommission: BigDecimal, currency: Currency) extends Event

  final case class WithholdTaxCharged(dateTime: LocalDateTime, broker: Broker, value: BigDecimal, currency: Currency) extends Event

  final case class DividendPaid(dateTime: LocalDateTime, broker: Broker, instrument: Instrument, value: BigDecimal, currency: Currency) extends Event

}
