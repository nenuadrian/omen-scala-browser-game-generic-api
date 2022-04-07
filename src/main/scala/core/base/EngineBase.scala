package core.base

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import core.api.Endpoints
import core.util.TimeProvider
import model._
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContextExecutor

abstract class EngineBase(val config: EngineConfig, leaderboardAgent: (Entity, List[Entity]) => List[(String, Int)], cronsEnabled: Boolean = true)(implicit db: BasicDataSource, timeProvider: TimeProvider) extends Logging {
  logger.info("""
                | _______  __   __  _______  __    _
                ||       ||  |_|  ||       ||  |  | |
                ||   _   ||       ||    ___||   |_| |
                ||  | |  ||       ||   |___ |       |
                ||  |_|  ||       ||    ___||  _    |
                ||       || ||_|| ||   |___ | | |   |
                ||_______||_|   |_||_______||_|  |__|""".stripMargin)

  logger.info(s"${config.entities.size} entities configured")

  implicit val system: ActorSystem = ActorSystem("omen-as")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val webRoutes = new Endpoints(this)

  def ackTask(task: Task): Task

  def tasks(playerId: Option[String], parentEntityId: Option[String], acknowledged: Boolean): List[Task]

  def tasksFor(entityId: String): List[Task]

  def task(taskId: String): Option[Task]

  def createTask(entity_id: String, req: CreateTaskRequest): Task

  def createEntityForRequest(createEntityRequest: CreateEntityRequest): EntityCreationResponse

  def updateRefData(entity: Entity, ref_key: String, ref_value: String): RefData

  def upgradeEntity(entity: Entity, to: Int): Entity

  def updateAttributeRequest(entity: Entity, attribute: Attribute): Attribute

  def allEntitiesWithId(id: String): List[Entity]

  def allEntitiesWithId(ids: List[String]): List[Entity]

  def entityWithEntityId(id: String, playerId: Option[String] = None): Option[Entity]

  def allEntitiesWithEntityId(ids: List[String]): List[Entity]

  def entityBy(player_id: String, entity_id: String): Entity

  def computeRequirements(entity: Entity, forAmount: Int): RequirementsResponse

  def entitiesWithPrimaryParent(player_id: String): List[Entity]

  def entitiesWithPlayerId(primaryParentEntityId: Option[String] = None, parent_entity_id: Option[String] = None): List[Entity]

  def leaderboard(forId: String): List[(String, List[(String, Int)])]

}


