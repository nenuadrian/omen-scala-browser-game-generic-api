package storage

import core.base.StorageEngine
import core.util.TimeProvider
import model._

class NoSQLDatabase(implicit timeProvider: TimeProvider, config: EngineConfig) extends StorageEngine {
  override def tasks(ids: List[String], entities: List[String]): List[Task] = ???

  override def entitiesWithPlayerId(primaryParentEntityId: Option[String], parent_entity_id: Option[String]): List[Entity] = ???

  override def attributesForEntities(entities: List[String]): Map[String, List[Attribute]] = ???

  override def put[T](e: T): T = ???

  override def entities(ids: List[String]): List[Entity] = ???

  override def save[T](e: T): T = ???

  override def updateFinishedTasks(): Unit = ???

  override def tasksWithPlayerId(playerId: Option[String], entityId: Option[String], acknowledged: Boolean): List[Task] = ???
}
