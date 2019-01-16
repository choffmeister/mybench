package de.kddc.mybench

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.softwaremill.macwire._
import com.typesafe.scalalogging.LazyLogging
import de.kddc.mybench.repositories.BenchRepository

import scala.util.{ Failure, Success }

trait ServiceComponents {
  this: ServiceComponentsBase with MongoDbComponentsBase =>
  lazy val benchRepository = wire[BenchRepository]
  lazy val httpServer = wire[HttpServer]
}

class Service(implicit val actorSystem: ActorSystem)
  extends DefaultServiceComponents
  with DefaultMongoDbComponents
  with ServiceComponents
  with LazyLogging {
  def start(): Unit = {
    val httpConfig = config.getConfig("http")
    val interface = httpConfig.getString("interface")
    val port = httpConfig.getInt("port")

    Http().bindAndHandle(httpServer.routes, interface, port).onComplete {
      case Success(binding) =>
        logger.info(s"Successfully bound to [${binding.localAddress}]")
      case Failure(error) =>
        logger.error("Binding failed", error)
        System.exit(1)
    }
  }
}
