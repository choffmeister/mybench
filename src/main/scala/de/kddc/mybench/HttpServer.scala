package de.kddc.mybench

import java.util.UUID

import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.CustomHeader
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.{Directive1, MalformedQueryParamRejection}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import de.choffmeister.microserviceutils.json.UUIDJsonProtocol
import de.kddc.mybench.repositories.BenchRepository
import de.kddc.mybench.repositories.BenchRepository._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import de.choffmeister.microserviceutils.json.RootJsonFormatWithDefault._

import scala.concurrent.{ExecutionContext, Future}

object HttpServerJsonProtocol extends DefaultJsonProtocol with UUIDJsonProtocol {
  implicit val LocationJsonFormat: RootJsonFormat[Location] = jsonFormat2(Location)
  implicit val BenchJsonFormat: RootJsonFormat[Bench] = jsonFormat3(Bench)
    .withDefault("_id", UUID.fromString("00000000-0000-0000-0000-000000000000"))
}

class HttpServer(benchRepository: BenchRepository)(implicit executionContext: ExecutionContext, materializer: ActorMaterializer) extends SprayJsonSupport {
  import HttpServerJsonProtocol._

  def listBenchesRoute = pathEnd {
    get {
      paging(10) { case (from, limit) =>
        val benchesF = benchRepository.all(from, limit)
        onSuccess(benchesF) { case (benches, count) =>
          respondWithHeader(`X-Total`(count)) {
            complete(benches)
          }
        }
      }
    }
  }

  def listBenchesRouteStreaming = path("stream") {
    get {
      val benchesS = benchRepository.all

      implicit val jsonStreamingSupport: JsonEntityStreamingSupport =
        EntityStreamingSupport.json()
          .withParallelMarshalling(parallelism = 8, unordered = false)

      complete(benchesS)
    }
  }

  // will block eventually because incoming messages are not consumed properly
  def listBenchesRouteWebsocket = path("websocket") {
    get {
      val receiving = Sink.ignore
      val benchesF = benchRepository.all
      val sending = benchesF.map { bench =>
        val json = BenchJsonFormat.write(bench)
        TextMessage(json.compactPrint)
      }
      val flow = Flow.fromSinkAndSource(receiving, sending)
      handleWebSocketMessages(flow)
    }
  }

  def retrieveBenchRoute = path(JavaUUID) { id =>
    get {
      onSuccessAndDefined(benchRepository.findById(id)) { bench =>
        complete(bench)
      }
    }
  }

  def createBenchRoute = pathEnd {
    post {
      entity(as[Bench]) { body =>
        onSuccess(benchRepository.create(body.copy(_id = UUID.randomUUID))) { bench =>
          complete(bench)
        }
      }
    }
  }

  def routes = pathPrefix("benches")(concat(listBenchesRoute, listBenchesRouteStreaming, listBenchesRouteWebsocket, retrieveBenchRoute, createBenchRoute))

  def onSuccessAndDefined[T](res: Future[Option[T]]): Directive1[T] = {
    onSuccess(res).flatMap {
      case Some(value) => provide(value)
      case None => reject
    }
  }

  def paging(defaultLimit: Int): Directive1[(Int, Int)] = {
    parameters('from.as[Int].?, 'limit.as[Int].?).tflatMap {
      case (from, _) if from.exists(_ < 0) =>
        reject(MalformedQueryParamRejection("from", "Must not be negative"))
      case (from, limit) =>
        provide((
          from.getOrElse(0),
          limit.getOrElse(defaultLimit)
        ))
    }
  }
}

final case class `X-Total`(total: Long) extends CustomHeader {
  override val name: String = "X-Total"
  override val value: String = total.toString
  override val renderInRequests: Boolean = false
  override val renderInResponses: Boolean = true
}

