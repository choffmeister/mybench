package de.kddc.mybench

import java.security.MessageDigest

import akka.actor.{Actor, ActorSystem, Cancellable, Props}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, StreamConverters}
import akka.util.ByteString
import com.softwaremill.macwire._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object MailSender {
  private case object Tick
  case object Init
  case object Ack
  case object Complete
  case class Failed(err: Throwable)
}

class MailSender extends Actor {
  import MailSender._

  var queue = List.empty[Mail]
  var schedule = Option.empty[Cancellable]
  override def receive: Receive = {
    case mail @ Mail(recipient, subject, body) =>
      println(s"ENQUEUED MAIL $mail")
      queue = queue :+ mail
      self ! Tick
      sender() ! Ack

    case Tick =>
      println("TICK")
      if (queue.nonEmpty) {
        val mail = queue.head
        queue = queue.tail
        if (Math.random() <= 0.5) {
          println(s"SENT MAIL $mail")
          self ! Tick
        } else {
          println(s"FAILURE")
          queue = queue :+ mail
          scheduleTick()
        }
      } else {
        scheduleTick()
      }

    case Init =>
      sender() ! Ack

    case Complete =>
      println(s"complete")

    case msg =>
      println(s"unexpected $msg")
  }

  private def scheduleTick(): Unit = {
    schedule.foreach(_.cancel())
    schedule = Some(context.system.scheduler.scheduleOnce(1.second, self, Tick)(context.dispatcher))
  }
}



case class Mail(recipient: String, subject: String, body: String)

object Application {
  import MailSender._

  def main(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem("mybench")
    implicit val executor = ExecutionContext.Implicits.global
    implicit val materializer = ActorMaterializer()
    val service = wire[Service]
    service.start()

    val sender = actorSystem.actorOf(Props[MailSender])
    val mailgunQueue = Source(1 to 2)
      .throttle(1, 1.second, 1, ThrottleMode.shaping)
      .map(i => Mail(s"user$i@domain.com", s"Mail $i", ""))
      .to(Sink.actorRefWithAck(sender, Init, Ack, Complete, err => Failed(err)))
      .run()


  }
}
