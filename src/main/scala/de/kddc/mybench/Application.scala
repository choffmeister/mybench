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

    val bytes = Source(0L until 1024L * 1024L * 1024L)
      .map(i => (i % 256).toByte)
      .grouped(1024)
      .map(bs => ByteString(bs.toArray))

    bytes
      .viaMat(hashFlow("SHA-1"))(Keep.right)
      .map { chunk =>
        println(s"chunk $chunk")
        chunk
      }
      .mapMaterializedValue { hashF =>
        hashF.onComplete {
          case Success(hash) => println(s"hash $hash")
          case Failure(error) => println(s"error $error")
        }
      }
      .runWith(Sink.ignore)

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
