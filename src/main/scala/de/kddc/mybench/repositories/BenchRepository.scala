package de.kddc.mybench.repositories

import java.util.UUID

import akka.stream.Materializer
import akka.{Done, NotUsed}
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import de.choffmeister.microserviceutils.mongodb.bson.{InstantBSONProtocol, UUIDBSONProtocol}
import reactivemongo.api.{Cursor, DefaultDB}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._
import reactivemongo.akkastream.{AkkaStreamCursor, State, cursorProducer}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class BenchRepository(db: DefaultDB)(implicit ec: ExecutionContext, mat: Materializer) {
  import BenchRepository.BSONProtocol._
  import BenchRepository._

  private val collection = Await.result(init(db), 10.seconds)

  def all: Source[Bench, NotUsed] = {
    val selector = BSONDocument.empty
    collection
      .find(selector)
      .cursor[Bench]()
      .documentSource()
      .mapMaterializedValue(_ => NotUsed)
  }

  def all(from: Int = 0, limit: Int = 10): Future[(Seq[Bench], Long)] = {
    val selector = BSONDocument.empty
    for {
      elems <- collection
        .find(selector)
        .skip(from)
        .cursor[Bench]()
        .collect[Seq](limit, Cursor.FailOnError[Seq[Bench]]())
      count <- collection.count(Some(selector))
    } yield (elems, count)
  }

  def findById(id: UUID): Future[Option[Bench]] = {
    collection.find(BSONDocument("_id" -> id)).one[Bench]
  }

  def findByName(name: String): Future[Option[Bench]] = {
    collection.find(BSONDocument("name" -> name)).one[Bench]
  }
}

object BenchRepository extends LazyLogging {
  final case class Location(longitude: Double, latitude: Double)
  final case class Bench(_id: UUID = UUID.randomUUID, name: String, location: Location)

  private def init(db: DefaultDB)(implicit ec: ExecutionContext): Future[BSONCollection] = {
    val collection = db.collection[BSONCollection](collectionName)
    for {
      collections <- db.collectionNames
      _ <- if (!collections.contains(collection.name)) for {
        _ <- collection.create()
        _ <- prefill(collection)
      } yield Done
      else Future.successful(Done)
    } yield collection
  }

  private def prefill(collection: BSONCollection)(implicit ec: ExecutionContext): Future[Done] = {
    import BSONProtocol._
    val benches = (1 to 10).map(i => Bench(name = s"Bench #$i", location = Location(Math.random() * 180 - 90, Math.random() * 180 - 90)))
    Future.sequence(benches.map { bench =>
      logger.info(s"Adding [$bench] to database")
      collection.insert(bench)
    }).map(_ => Done)
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