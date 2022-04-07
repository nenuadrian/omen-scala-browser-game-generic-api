package core.base

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import core.api.Endpoints
import core.util.TimeProvider
import model._
import org.neo4j.dbms.api.{DatabaseManagementService, DatabaseManagementServiceBuilder}
import org.apache.logging.log4j.scala.Logging
import org.neo4j.graphdb.GraphDatabaseService

import java.io.File
import scala.concurrent.ExecutionContextExecutor

abstract class EngineBase(val config: EngineConfig, leaderboardAgent:
  (Entity, List[Entity]) => List[(String, Int)], cronsEnabled: Boolean = true)
                         (implicit storageEngine: StorageEngine, timeProvider: TimeProvider) extends Logging {
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

  // https://github.com/neo4j/neo4j-documentation/blob/4.4/embedded-examples/src/main/java/org/neo4j/examples/EmbeddedNeo4j.java
  //val managementService: DatabaseManagementService = new DatabaseManagementServiceBuilder(new File(System.getProperty("java.io.tmpdir")).toPath).build
  //val graphDb: GraphDatabaseService = managementService.database("omen")

  def createTask(entity_id: String, req: CreateTaskRequest): Task
  def createEntity(createEntityRequest: CreateEntityRequest): EntityCreationResponse

  def entityWithEntityId(id: String, playerId: Option[String] = None): Option[Entity]
  def entitiesWithPlayerId(primaryParentEntityId: Option[String] = None, parent_entity_id: Option[String] = None): List[Entity]

  def ackTask(task: Task): Task
  def tasks(playerId: Option[String], parentEntityId: Option[String], acknowledged: Boolean): List[Task]
  def task(taskId: String): Option[Task]
  def updateRefData(entity: Entity, ref_key: String, ref_value: String): RefData
  def upgradeEntity(entity: Entity, to: Int): Entity
  def updateAttributeRequest(entity: Entity, attribute: Attribute): Attribute
  def computeRequirements(entity: Entity, forAmount: Int): RequirementsResponse

  def leaderboard(forId: String): List[(String, List[(String, Int)])]
}


