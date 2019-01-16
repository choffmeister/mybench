package de.kddc.mybench

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver }

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._

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

trait MongoDbComponentsBase {
  def mongoDbDatabaseName: String
  def mongoDb: DefaultDB
}

trait DefaultMongoDbComponents extends MongoDbComponentsBase { this: ServiceComponentsBase =>
  private lazy val uri = MongoConnection.parseURI(config.getString("mongodb.uri")).get
  private lazy val driver = MongoDriver(config, getClass.getClassLoader)
  private lazy val mongoDbConnection = driver.connection(uri)
  lazy val mongoDbDatabaseName = config.getString("mongodb.database-name")
  lazy val mongoDb = Await.result(mongoDbConnection.database(mongoDbDatabaseName), 10.seconds)
}
