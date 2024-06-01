package persistence

import model.Model.{Broker, Currency, Instrument}

import java.time.LocalDateTime

object Model {

  sealed trait Event

  case class DepositMade(dateTime: LocalDateTime, broker: Broker, value: BigDecimal, currency: Currency) extends Event

  case class WithdrawalDone(dateTime: LocalDateTime, broker: Broker, value: BigDecimal, currency: Currency) extends Event

  case class AssetBought(dateTime: LocalDateTime, broker: Broker, instrument: Instrument, amount: Int,
                         totalPrice: BigDecimal, totalCommission: BigDecimal, currency: Currency) extends Event

  case class AssetSold(dateTime: LocalDateTime, broker: Broker, instrument: Instrument, amount: Int,
                       totalPrice: BigDecimal, totalCommission: BigDecimal, currency: Currency) extends Event

  case class WitholdTaxCharged(dateTime: LocalDateTime, broker: Broker, value: BigDecimal, currency: Currency) extends Event

  case class DividendPaid(dateTime: LocalDateTime, broker: Broker, instrument: Instrument, value: BigDecimal, currency: Currency) extends Event

}
