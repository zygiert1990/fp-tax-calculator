package com.zygiert.persistence

import cats.effect.kernel.{Concurrent, Resource}
import cats.implicits._
import com.zygiert.persistence.Model.Event
import io.circe.generic.auto._
import mongo4cats.circe._
import mongo4cats.client.MongoClient

trait EventRepository[F[_]] {

  val eventRepository: Repository

  trait Repository {
    def saveAll(events: List[Event]): F[Unit]

    def findAll: F[List[Event]]
  }

}

class LiveEventRepository[F[_]: Concurrent](mongoClient: Resource[F, MongoClient[F]]) extends EventRepository[F] {
  override val eventRepository: Repository = new Repository {
    override def saveAll(events: List[Event]): F[Unit] =
      mongoClient.use { client =>
        for {
          db   <- client.getDatabase("local")
          coll <- db.getCollectionWithCodec[Event]("events")
          _    <- coll.insertMany(events)
        } yield ()
      }

    override def findAll: F[List[Event]] =
      mongoClient.use { client =>
        for {
          db      <- client.getDatabase("local")
          coll    <- db.getCollectionWithCodec[Event]("events")
          events  <- coll.find.all
        } yield events.toList
      }
  }
}
