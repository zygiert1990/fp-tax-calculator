package com.zygiert.persistence

import cats.effect.IO
import cats.effect.kernel.Resource
import com.zygiert.model.Model.Broker
import com.zygiert.persistence.Model.Event
import io.circe.derivation.Configuration
import io.circe.generic.auto.*
import mongo4cats.circe.*
import mongo4cats.client.MongoClient
import mongo4cats.operations.Projection

trait EventRepository {

  def saveAll(events: List[Event]): IO[Unit]

  def findAll: IO[List[Event]]

}

class EventRepositoryImpl private(private val mongoClient: Resource[IO, MongoClient[IO]]) extends EventRepository {
  override def saveAll(events: List[Event]): IO[Unit] =
    mongoClient.use { client =>
      for {
        db <- client.getDatabase("local")
        coll <- db.getCollectionWithCodec[Event]("events")
        _ <- coll.insertMany(events)
      } yield ()
    }

  override def findAll: IO[List[Event]] =
    mongoClient.use { client =>
      for {
        db <- client.getDatabase("local")
        coll <- db.getCollectionWithCodec[Event]("events")
        events <- coll.find.projection(Projection.excludeId).all
      } yield events.toList
    }
}

object EventRepositoryImpl {
  def apply(mongoClient: Resource[IO, MongoClient[IO]]): EventRepository = new EventRepositoryImpl(mongoClient)
}