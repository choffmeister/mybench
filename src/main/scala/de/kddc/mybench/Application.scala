package de.kddc.mybench

import java.security.MessageDigest

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, StreamConverters}
import akka.util.ByteString
import com.softwaremill.macwire._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object Application {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem("mybench")
    implicit val executor = ExecutionContext.Implicits.global
    implicit val materializer = ActorMaterializer()
    val service = wire[Service]
    service.start()

    val bytes = Source(0 until 10 * 1024)
      .map(i => (i % 256).toByte)
      .grouped(1024)
      .map(bs => ByteString(bs.toArray))

    bytes
      .viaMat(hashFlow("SHA-1"))(Keep.right)
      .viaMat(countFlow())((l, r) => l.zip(r))
      .map { chunk =>
        println(s"chunk $chunk")
        chunk
      }
      .mapMaterializedValue { hashCountF =>
        hashCountF.onComplete {
          case Success(hash) => println(s"hash and count $hash")
          case Failure(error) => println(s"error $error")
        }
      }
      .runWith(Sink.ignore)

    def countFlow[T](): Flow[T, T, Future[Long]] = {
      var counter = 0L
      Flow[T]
        .map { elem =>
          counter = counter + 1
          elem
        }
        .watchTermination() { (_, done) =>
          done.map(_ => counter)
        }
    }

    def hashFlow(algorithm: String): Flow[ByteString, ByteString, Future[ByteString]] = {
      val digest = MessageDigest.getInstance(algorithm)
      Flow[ByteString]
        .map { chunk =>
          digest.update(chunk.toArray)
          chunk
        }
        .watchTermination() { (_, done) =>
          done.map { _ =>
            ByteString(digest.digest())
          }
        }
    }
  }
}
