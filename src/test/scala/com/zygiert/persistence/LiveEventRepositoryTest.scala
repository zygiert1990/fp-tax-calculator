package com.zygiert.persistence

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.zygiert.model.Model.{Broker, Currency}
import com.zygiert.persistence.Model.{DepositMade, WithdrawalDone}
import mongo4cats.client.MongoClient
import mongo4cats.embedded.EmbeddedMongo
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import java.time.LocalDateTime

class LiveEventRepositoryTest extends AsyncWordSpec with Matchers with EmbeddedMongo {

  override val mongoPort: Int = 12345

  "EventRepository" should {
    "create and retrieve documents from a db" in withRunningEmbeddedMongo {
      // given
      val client = MongoClient.fromConnectionString[IO]("mongodb://localhost:12345")
      val repository = new LiveEventRepository[IO](client).eventRepository
      val events = List(
        DepositMade(LocalDateTime.now(), Broker("broker"), BigDecimal(10), Currency("USD")),
        WithdrawalDone(LocalDateTime.now(), Broker("broker"), BigDecimal(10), Currency("USD")))
      // when && then
      for {
        _ <- repository.saveAll(events)
        result <- repository.findAll
      } yield result mustBe events
    }.unsafeToFuture()

  }
}
