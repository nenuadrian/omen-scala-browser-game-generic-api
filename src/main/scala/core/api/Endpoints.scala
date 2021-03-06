package core.api

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directive1, Route, StandardRoute}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import core.base.EngineBase
import core.impl.Engine
import model.json.JsonSupport._
import model._
import org.apache.logging.log4j.scala.Logging
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps

class Endpoints(omen: EngineBase)(implicit system: ActorSystem, materializer: ActorMaterializer,
                              executionContext: ExecutionContextExecutor)
  extends Logging with CORSSupport with SprayJsonSupport with DefaultJsonProtocol {
  implicit val timeout: Timeout = Timeout(10000 seconds)

  import model.AttributeProtocol._
  import model.EntityProtocol._
  import model.RefDataProtocol._
  import model.TaskProtocol._

  private def response(foo: () => JsValue): StandardRoute = {
    complete(try {
      JsObject("status" -> JsNumber(200), "data" -> foo())
    } catch {
      case e: Throwable => {
        logger.error(e.getMessage, e)
        JsObject("status" -> JsNumber(500), "error" -> JsString(e.getMessage))
      }
    })
  }

  implicit def tojs[T](a: T)(implicit writer : spray.json.JsonWriter[T]) : spray.json.JsValue = writer.write(a)

  val route: Route = respondWithCORS {
    pathSingleSlash {
      response(() => true)
    } ~ path("ping") {
      response(() => true)
    } ~ path("configuration") {
      response(() => omen.config)
    } ~ path("player") {
      put {
        response(() => omen.createEntity(CreateEntityRequest("players", None, None)))
      }
    } ~ pathPrefix("entities") {
        parameter("primaryParentEntityId" ?) { primaryParentEntityId => {
          pathPrefix(Segment) { entity_id => {
            val currentEntityOptional = omen.entityWithEntityId(entity_id, primaryParentEntityId)
            currentEntityOptional match {
              case Some(currentEntity: Entity) => {
                path("upgrade" / IntNumber) { to =>
                  response(() => omen.upgradeEntity(currentEntity, to))
                } ~ path("ref" / Segment / Segment) { (ref_key, ref_value) =>
                  post {
                    response(() => omen.updateRefData(currentEntity, ref_key, ref_value))
                  }
                } ~ path("attributes" / Segment / Segment) { (attrName, value) =>
                  post {
                    response(() => omen.updateAttributeRequest(currentEntity, Attribute(attrName, entity_id, Some(value), 0)))
                  }
                } ~ path("requirements") {
                  parameter("amount"?) { forAmount => {
                    post {
                      response(() => {
                          val requirements = omen.computeRequirements(currentEntity, forAmount.getOrElse("1").toInt)
                          if (requirements.fulfilled) {
                            //todo how about entities aggregateable sacrifice?
                            requirements.attributes.foreach(attr => {
                              val id = attr.requirement.id.split("\\[").last.split("]").head
                              val parentEntity = omen.entityWithEntityId(currentEntity.parent_entity_id.get, primaryParentEntityId).get

                              val e = if (attr.requirement.id.startsWith("parent[")) {
                                parentEntity
                              } else {
                                omen.entityWithEntityId(parentEntity.parent_entity_id.get, primaryParentEntityId).get
                              }

                              e.attributes.find(_.attr == id) match {
                                case Some(curAttr) => {
                                  omen.updateAttributeRequest(e, curAttr.copy(value = curAttr.value.map(v => v.toDouble - attr.requirement.formula.get.toDouble).map(_.toString)))
                                }
                                case _ => throw new Exception(s"not found ${id}")
                              }
                            })
                            JsNull
                          } else {
                            throw new RuntimeException("Not fulfilled")
                          }
                      })

                    } ~ get {
                      response(() => omen.computeRequirements(currentEntity, forAmount.getOrElse("1").toInt))
                    }
                  }}
                } ~ pathEnd {
                  response(() => currentEntity)
                }
              }
              case _ => pathEnd { complete(JsObject("status" -> JsNumber(404))) }
            }
       }} ~ get {
          response(() => omen.entitiesWithPlayerId(primaryParentEntityId))
        }
       }} ~ pathEnd {
         post {
          entity(as[EntitiesQuery]) { query =>
            response(() => omen.entitiesWithPlayerId(query.primaryParentEntityId, query.parent_entity_id)
              .filter(e => query.tag match {
                case Some(_) => omen.config.entities.find(_.id == e.id).flatMap(_.tag) == query.tag
                case _ => true
              }).filterNot(e => query.refDataFilters match {
              case None => false
              case Some(filters) => filters.exists(f => !e.refData.exists(rd => rd.ref_key == f.key && rd.ref_value == f.value))
            }))
          }
        } ~ put { entity(as[CreateEntityRequest]) { request =>
            response(() => omen.createEntity(request))
        }}
      }
    } ~ pathPrefix("tasks") {
      pathPrefix(Segment) { task_id => {
        val task = omen.task(task_id)
        path("ack") {
          response(() => {
            task match {
              case Some(t) => omen.ackTask(t)
              case _ => JsObject("status" -> JsNumber(404))
            }
          })
        }
      }} ~ pathEnd {
        parameter("player_id" ?, "parent_entity_id" ?, "acknowledged" ? false) { (playerId, parentEntityId, acknowledged) =>
          put {
            entity(as[CreateTaskRequest]) { request =>
              response(() => omen.createTask(parentEntityId.get, request))
            }
          } ~ get {
            response(() => omen.tasks(playerId, parentEntityId, acknowledged))
          }
        }
      }
    } ~ pathPrefix("leaderboard") {
      parameter("id") { forId => {
        get {
          response(() => omen.leaderboard(forId))
        }
      }}
    } ~ pathPrefix("tech-tree") {
      path("html") {
        getFromResource("html/tech.html") // uses implicit ContentTypeResolver
      } ~ pathEnd {
        get {
          response(() => omen.config.toGraph)
        }
      }
    }
  }
}