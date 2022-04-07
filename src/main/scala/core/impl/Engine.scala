package core.impl

import core.base.{EngineBase, StorageEngine}
import core.util.TimeProvider
import model._
import org.nfunk.jep.JEP
import spray.json.JsNull

import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import scala.concurrent.duration.Duration

class Engine(config: EngineConfig, leaderboardAgent: (Entity, List[Entity]) => List[(String, Int)], cronsEnabled: Boolean = true)
            (implicit storageEngine: StorageEngine, timeProvider: TimeProvider)
  extends EngineBase(config, leaderboardAgent, cronsEnabled)(storageEngine, timeProvider) {

  private val scheduler = system.scheduler
  private val hourlyAttributes: Seq[(EntityConfig, AttributeConfig)] = config.entities.filter(_.have.isDefined).flatMap(e => e.have.get.filter(_.hourly_rate_attribute.isDefined).map(h => e -> h))
  private val tasksTask = new Runnable {
    def run(): Unit = executeTasksTask()
  }
  private val hourlyRateTask = new Runnable {
    def run(): Unit = executeHourlyRateUpdate()
  }

  def executeTasksTask(): Unit = {
    storageEngine.updateFinishedTasks()
  }

  def executeHourlyRateUpdate(): Unit = {
    val entities = allEntitiesWithId(hourlyAttributes.map(_._1.id).toList)
    entities.foreach(entity => {
      val attributes = hourlyAttributes.filter(_._1.id == entity.id).map(_._2)
      entity.attributes.map(a => (a, attributes.find(_.id == a.attr).flatMap(_.hourly_rate_attribute))).flatMap {
        case (entityAttribute, Some(hourlyAttribute)) => {
          Some(entityAttribute.copy(value = Some({
            var finalValue = entityAttribute.value.get.toDouble
            val hourlyRate = entity.attributes.find(aa => aa.attr == hourlyAttribute).get.value.get.toFloat
            val period = (timeProvider.currentTimestamp - entityAttribute.lastHourlyTimestamp) / 1000
            finalValue = finalValue + ((hourlyRate / 60) * period).toDouble
            finalValue.toString
          }), lastHourlyTimestamp = timeProvider.currentTimestamp))
        }
        case _ => None
      }.foreach(entity.updateAttribute)
    })
  }

  if (cronsEnabled) {
    scheduler.schedule(
      initialDelay = Duration(0, TimeUnit.SECONDS),
      interval = Duration(1, TimeUnit.SECONDS),
      runnable = tasksTask)

    scheduler.schedule(
      initialDelay = Duration(0, TimeUnit.SECONDS),
      interval = Duration(1, TimeUnit.SECONDS),
      runnable = hourlyRateTask)
  }

  def ackTask(task: Task): Task = storageEngine.save(task.ack())

  def tasks(playerId: Option[String], parentEntityId: Option[String], acknowledged: Boolean): List[Task] = {
    storageEngine.tasksWithPlayerId(playerId, parentEntityId, acknowledged)
  }

  def tasksFor(entityId: String): List[Task] = {
    storageEngine.tasks(entities = List(entityId))
  }

  def task(taskId: String): Option[Task] = {
    storageEngine.tasks(ids = List(taskId)).headOption
  }

  def createTask(entity_id: String, req: CreateTaskRequest): Task = {
    val task = Task(java.util.UUID.randomUUID.toString, entity_id, req.duration,
      timeProvider.currentTimestamp + 1000 * req.duration, acknowledged = false, finished = false, data = req.data.getOrElse(JsNull))

    storageEngine.put(task)
    task
  }

  def createEntity(createEntityRequest: CreateEntityRequest): EntityCreationResponse = {
    logger.info("Creating new entity")

    val entityDescription = config.entityConfigById(createEntityRequest.id).get

    val entity = (entityDescription.aggregateable, createEntityRequest.entity_primary_parent) match {
      case (Some(true), Some(entity_primary_parent)) => {
        entitiesWithPrimaryParent(entity_primary_parent).find(_.id == createEntityRequest.id) match {
          case Some(e) => storageEngine.save(e.copy(amount = e.amount + 1))
          case _ => createEntity(createEntityRequest, entityDescription)
        }
      }
      case _ => createEntity(createEntityRequest, entityDescription)
    }

    entityDescription.own.foreach(
      _.map(o => config.entityConfigById(o).get).foreach(ed =>
            createEntity(CreateEntityRequest(ed.id,
              Some(entity.entity_id),
              createEntityRequest.entity_primary_parent))
        ))

    logger.info(s"Created new entity ${entity.entity_id}")

    EntityCreationResponse(entity.entity_id)
  }

  def updateRefData(entity: Entity, ref_key: String, ref_value: String): RefData = {
    entity.refData.find(_.ref_key == ref_key) match {
      case Some(ref) => storageEngine.save(ref.copy(ref_value = ref_value))
      case _ => storageEngine.put(RefData(entity.entity_id, ref_key, ref_value))
    }
  }

  def upgradeEntity(entity: Entity, to: Int): Entity = {
    storageEngine.save(entity.copy(amount = entity.amount + 1))
  }

  def updateAttributeRequest(entity: Entity, attribute: Attribute): Attribute = {
     entity.updateAttribute(attribute)
  }

  def allEntitiesWithId(id: String): List[Entity] = allEntitiesWithId(List(id))

  def allEntitiesWithId(ids: List[String]): List[Entity] = {
    if (ids.isEmpty) {
      List()
    } else {
      storageEngine.entities(ids = ids)
    }
  }

  def entityWithEntityId(id: String, playerId: Option[String] = None): Option[Entity] =
    allEntitiesWithEntityId(List(id)).find(e => playerId match {
      case None => true
      case _ => e.primary_parent_entity_id.contains(playerId.getOrElse(e.entity_id))
    })

  def allEntitiesWithEntityId(ids: List[String]): List[Entity] = {
    if (ids.isEmpty) {
      List()
    } else {
      storageEngine.entities(ids)
    }
  }

  def entityBy(player_id: String, entity_id: String): Entity = {
    entitiesWithPrimaryParent(player_id).find(e => e.entity_id == entity_id || e.primary_parent_entity_id.getOrElse("") == entity_id).get
  }

  def computeRequirements(entity: Entity, forAmount: Int): RequirementsResponse = {
    val entityDescription = config.entityConfigById(entity.id).get
    val playerEntities = entity.primary_parent_entity_id match {
      case Some(primary_parent_entity_id) => entitiesWithPrimaryParent(primary_parent_entity_id)
      case None => List()
    }
    val entityEntities: List[Entity] = playerEntities.filter(e => e.parent_entity_id.contains(entity.id))
    val parentEntity: Option[Entity] = entity.parent_entity_id.flatMap(pei => entity.primary_parent_entity_id match {
      case Some(primary_parent_entity_id) => Some(entityBy(primary_parent_entity_id, pei))
      case None => None
    })
    val parentEntityEntities: List[Entity] = playerEntities.filter(e => e.parent_entity_id == entity.parent_entity_id)
    val parentParentEntity: Option[Entity] = parentEntity.map(_.parent_entity_id)
      .flatMap(pei => pei.flatMap(peii => entity.primary_parent_entity_id match {
        case Some(primary_parent_entity_id) => Some(entityBy(primary_parent_entity_id, peii))
        case None => None
      }))

    val parentParentEntityEntities: List[Entity] = playerEntities.filter(e => e.parent_entity_id == parentEntity.flatMap(_.parent_entity_id))

    val entityReplacements = (
      Some(parentParentEntityEntities.map(e => Pattern.quote("parent.parent[" + e.id + "]") -> e.amount.toString)) ++
        Some(parentEntityEntities.map(e => Pattern.quote("parent[" + e.id + "]") -> e.amount.toString)) ++
        Some(entityEntities.map(e => Pattern.quote("[" + e.id + "]") -> e.amount.toString)) ++
        parentParentEntity.map(_.attributes.map(a => Pattern.quote("parent.parent[" + a.attr + "]") -> a.value.get).toMap) ++
        parentEntity.map(_.attributes.map(a => Pattern.quote("parent[" + a.attr + "]") -> a.value.get).toMap) ++
        Some((Pattern.quote("[amount]") -> entity.amount.toString) :: entity.attributes.map(a => Pattern.quote("[" + a.attr + "]") -> a.value.get))
      ).flatten.toMap

    val parser = new JEP()
    parser.addStandardFunctions()
    parser.addStandardConstants()

    val result = entityDescription.requirements match {
      case Some(req) => {
        val attributes: List[RequirementResponse] = req.attributes.getOrElse(List()).map(r => processFormula(entityReplacements, parser, r, forAmount))
        val entities: List[RequirementResponse] = req.entities.getOrElse(List()).map(r => processFormula(entityReplacements, parser, r, forAmount))

        RequirementsResponse(
          attributes = attributes,
          entities = entities,
          have = List(),
          fulfilled = attributes.forall(_.fulfilled) && entities.forall(_.fulfilled)
        )
      }
      case _ => RequirementsResponse(List(), List(), List(), fulfilled = true)
    }

    result.copy(
      have = entityDescription.have.getOrElse(List()).map(r => processFormula(entityReplacements, parser, r, forAmount))
    )
  }

  private def processFormula(entityReplacements: Map[String, String], parser: JEP,
                             r: AttributeConfig, forAmount: Int): RequirementResponse = {

    var formula = r.formula.get
    entityReplacements.foreach(r => formula = formula.replaceAll(r._1, r._2))
    parser.parseExpression(formula)

    val finalValue = parser.getValue * forAmount
    RequirementResponse(
      current = entityReplacements.getOrElse(Pattern.quote(r.id), 0.toString),
      r.copy(formula = Some(finalValue.toString)),
      fulfilled = entityReplacements.contains(Pattern.quote(r.id)) && entityReplacements(Pattern.quote(r.id)).toDouble >= finalValue || !entityReplacements.contains(Pattern.quote(r.id)))
  }

  def entitiesWithPrimaryParent(player_id: String): List[Entity] = {
    entitiesWithPlayerId(Some(player_id), None)
  }

  def entitiesWithPlayerId(primaryParentEntityId: Option[String] = None, parent_entity_id: Option[String] = None): List[Entity] = {
    storageEngine.entitiesWithPlayerId(primaryParentEntityId, parent_entity_id)
  }

  private def createEntity(createEntityRequest: CreateEntityRequest, entityDescription: EntityConfig): Entity = {
    logger.info(s"Insert new entity ${entityDescription.id}")

    val entity = {
      val e = Entity(id = entityDescription.id,
        primary_parent_entity_id = createEntityRequest.entity_primary_parent,
        parent_entity_id = createEntityRequest.entity_parent, amount = 0)

      e.copy(parent_entity_id = e.parent_entity_id match {
        case Some(parent_entity_id) => Some(parent_entity_id)
        case _ => Some(e.entity_id)
      }, primary_parent_entity_id = e.primary_parent_entity_id match {
        case Some(primary_parent_entity_id) => Some(primary_parent_entity_id)
        case _ => Some(e.entity_id)
      })
    }

    storageEngine.put(entity)

    entityDescription.have.foreach(_.foreach(attributePair => {
      entity.updateAttribute(Attribute(attributePair.id, entity.entity_id, attributePair.default.map(_.toString), 0))
    }))

    logger.info(s"Inserted new entity with its attributes: ${entity.entity_id}")

    entity
  }

  def leaderboard(forId: String): List[(String, List[(String, Int)])] = {
    allEntitiesWithId(forId).par.map(i => (i.entity_id, {
      val entities = entitiesWithPlayerId(Some(i.entity_id))
      leaderboardAgent(i, entities)
    })).toList
  }

}

object Engine {
  sealed trait DependencyTree
  case class Node(name: String) extends DependencyTree
}
