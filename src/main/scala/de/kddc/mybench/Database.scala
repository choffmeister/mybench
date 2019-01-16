package de.kddc.mybench

import akka.NotUsed
import akka.stream.scaladsl.Source

import scala.concurrent.Future

case class User(id: Long)
case class Bench(id: Long, longitude: Double, latitude: Double)

class UserRepository {
  // TODO
}

class BenchRepository {
  def all: Source[Bench, NotUsed] = ???
  def findById(id: Long): Future[Option[Bench]] = ???
}
