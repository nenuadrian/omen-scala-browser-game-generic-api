package storage

import core.base.StorageEngine
import core.util.TimeProvider
import model._
import com.mongodb.client.{MongoClient, MongoClients, MongoDatabase}

class NoSQLDatabase(implicit timeProvider: TimeProvider, config: EngineConfig) extends StorageEngine {
  val uri: String = "mongodb+srv://<username>:<password>@<cluster-address>/test?retryWrites=true&w=majority"
  System.setProperty("org.mongodb.async.type", "netty")

  val mongoClient: MongoClient = MongoClients.create("mongodb://localhost:27017")
  val database: MongoDatabase = mongoClient.getDatabase("omen")
  database.createCollection("entities")
  database.createCollection("tasks")
  database.createCollection("messages")

  override def tasks(ids: List[String], entities: List[String]): List[Task] = ???

  override def entitiesWithPlayerId(primaryParentEntityId: Option[String], parent_entity_id: Option[String]): List[Entity] = ???

  override def attributesForEntities(entities: List[String]): Map[String, List[Attribute]] = ???

  override def put[T](e: T): T = ???

  override def entities(ids: List[String]): List[Entity] = {
    database.getCollection("entities")
    List()
  }

  override def save[T](e: T): T = ???

  override def updateFinishedTasks(): Unit = {

  }

  override def tasksWithPlayerId(playerId: Option[String], entityId: Option[String], acknowledged: Boolean): List[Task] = ???
}
