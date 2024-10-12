package com.zygiert.persistence

import cats.effect.IO
import com.zygiert.persistence.Model.Event

class TestEventRepository extends EventRepository {

  override def saveAll(events: List[Event]): IO[Unit] = IO.println {
    s"Successfully stored: ${events.size} events"
  }

  override def findAll: IO[List[Event]] = ???
}

object TestEventRepository {
  def apply(): EventRepository = {
    new TestEventRepository
  }
}
