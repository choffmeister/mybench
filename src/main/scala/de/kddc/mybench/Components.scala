package de.kddc.mybench

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

trait ServiceComponentsBase {
  implicit def actorSystem: ActorSystem
  implicit def executionContext: ExecutionContext
  implicit def materializer: ActorMaterializer
  def config: Config
}

trait DefaultServiceComponents extends ServiceComponentsBase {
  override implicit lazy val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override implicit lazy val materializer: ActorMaterializer = ActorMaterializer()(actorSystem)
  override lazy val config: Config = actorSystem.settings.config
}