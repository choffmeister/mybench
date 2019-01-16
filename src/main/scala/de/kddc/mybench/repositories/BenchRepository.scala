package de.kddc.mybench.repositories

import java.util.UUID

import akka.{Done, NotUsed}
import akka.stream.scaladsl.Source
import de.choffmeister.microserviceutils.mongodb.bson.{InstantBSONProtocol, UUIDBSONProtocol}
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class BenchRepository(db: DefaultDB)(implicit ec: ExecutionContext) {
  import BenchRepository.BSONProtocol._
  import BenchRepository._

  private val collection = Await.result(init(db), 10.seconds)

  def all: Source[Bench, NotUsed] = ???
  def findById(id: UUID): Future[Option[Bench]] = ???
}

object BenchRepository {
  final case class Location(longitude: Double, latitude: Double)
  final case class Bench(_id: UUID, name: String, location: Location)

  private def init(db: DefaultDB)(implicit ec: ExecutionContext): Future[BSONCollection] = {
    val collection = db.collection[BSONCollection](collectionName)
    for {
      collections <- db.collectionNames
      _ <- if (!collections.contains(collection.name)) for {
        _ <- collection.create()
      } yield Done
      else Future.successful(Done)
    } yield collection
  }

  val collectionName = "benches"
  object BSONProtocol extends UUIDBSONProtocol with InstantBSONProtocol {
    implicit val LocationHandler: BSONDocumentHandler[Location] = Macros.handler[Location]
    implicit val BenchHandler: BSONDocumentHandler[Bench] = Macros.handler[Bench]

//    implicit val BenchHandler = new BSONDocumentWriter[Bench] with BSONDocumentReader[Bench] {
//      override def read(bson: BSONDocument): Bench = ???
//      override def write(t: Bench): BSONDocument = BSONDocument(
//        "id" -> uuidReaderWriter.write(t.id),
//        "name" -> BSONString(t.name),
//      )
//    }
  }
}