package de.kddc.mybench

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.softwaremill.macwire._
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

case class Bench(id: Long, longitude: Double, latitude: Double)

object Application {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    implicit val actorSystem = ActorSystem("mybench", config)
    implicit val executorExecution = ExecutionContext.Implicits.global
    implicit val materializer = ActorMaterializer()
    implicit val database = new DatabaseDriver("fake")
    val service = wire[Service]
    service.start()
  }
}
