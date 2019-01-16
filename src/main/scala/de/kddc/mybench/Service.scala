package de.kddc.mybench

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.softwaremill.macwire._
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class Service(config: Config)(implicit actorSystem: ActorSystem, executorExecution: ExecutionContext, materializer: ActorMaterializer, database: DatabaseDriver) {
  val httpConfig = config.getConfig("http")
  val interface = httpConfig.getString("interface")
  val port = httpConfig.getInt("port")

  lazy val benchRepository = wire[BenchRepository]
  lazy val userRepository = wire[UserRepository]
  lazy val httpServer = wire[HttpServer]

  def start(): Unit = {
    Http().bindAndHandle(httpServer.routes, interface, port).onComplete {
      case Success(binding) =>
        println(s"Successfully bound to ${binding.localAddress}")
      case Failure(error) =>
        println(s"Binding failed\n$error")
        System.exit(1)
    }
  }
}
