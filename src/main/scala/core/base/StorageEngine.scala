package core.base

import model.{Attribute, Entity, Task}

abstract class StorageEngine {
  def tasks(ids: List[String] = List(), entities: List[String] = List()): List[Task]
  def entitiesWithPlayerId(primaryParentEntityId: Option[String], parent_entity_id: Option[String]): List[Entity]
  def attributesForEntities(entities: List[String]): Map[String, List[Attribute]]
  def put[T](e: T): T
  def entities(ids: List[String]): List[Entity]
  def save[T](e: T): T
  def updateFinishedTasks(): Unit
  def tasksWithPlayerId(playerId: Option[String], entityId: Option[String], acknowledged: Boolean = false): List[Task]
}
