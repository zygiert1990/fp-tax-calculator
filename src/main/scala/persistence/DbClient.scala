package persistence

import cats.effect.IO
import cats.effect.kernel.Resource
import mongo4cats.client.MongoClient
import persistence.Model.Event
import mongo4cats.circe._
import io.circe.generic.auto._

object DbClient {

  val mongoClient: Resource[IO, MongoClient[IO]] = MongoClient.fromConnectionString[IO]("mongodb://172.17.0.2:27017")

  def saveAll(events: List[Event]): IO[Unit] = {
    mongoClient.use { client =>
      for {
        db    <- client.getDatabase("local")
        coll  <- db.getCollectionWithCodec[Event]("events")
        _     <- coll.insertMany(events)
      } yield ()
    }
  }

}
