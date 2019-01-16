package de.kddc.mybench

import akka.NotUsed
import akka.stream.ThrottleMode
import akka.stream.scaladsl.Source

import scala.concurrent.duration._
import scala.concurrent.Future

class DatabaseDriver(connectionUri: String)

class UserRepository(db: DatabaseDriver) {
  // TODO
}

class BenchRepository(db: DatabaseDriver) {
  def all: Source[Bench, NotUsed] = {
    Source(1 to 10000000)
      .map { i =>
        println(i)
        i
      }
      .map(i => Bench(i, Math.random() * 90, Math.random() * 90))
      .throttle(1, 1000.millis, 1, ThrottleMode.shaping)
  }

  def findById(id: Long): Future[Option[Bench]] = {
    if (id < 100) {
      val bench = Bench(id, Math.random() * 90, Math.random() * 90)
      Future.successful(Some(bench))
    } else Future.successful(None)
  }
}
