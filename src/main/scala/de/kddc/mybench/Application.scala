package de.kddc.mybench

import akka.actor.ActorSystem
import com.softwaremill.macwire._

object Application {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem("mybench")
    val service = wire[Service]
    service.start()
  }
}
