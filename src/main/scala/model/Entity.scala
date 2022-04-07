package model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import core.base.StorageEngine
import core.util.TimeProvider
import spray.json._

import java.sql.{Connection, ResultSet}
import java.util.UUID.randomUUID
import scala.language.implicitConversions

case class CreateEntityRequest(id: String, entity_parent: Option[String], entity_primary_parent: Option[String])

case class EntityCreationResponse(entity_id: String)

case class Entity(entity_id: String = randomUUID.toString, id: String, attributes: List[Attribute] = List(),
                  refData: List[RefData] = List(), primary_parent_entity_id: Option[String], parent_entity_id: Option[String], amount: Int) {

  def attributes()(implicit store: StorageEngine): List[Attribute] = {
    store.attributesForEntities(List(entity_id)).values.headOption.getOrElse(List())
  }

  def updateAttribute(attribute: Attribute)(implicit store: StorageEngine, timeProvider: TimeProvider): Attribute = {
    attributes().find(_.attr == attribute.attr) match {
      case Some(_) => store.save(attribute)
      case _ => store.put(attribute)
    }
    attribute
  }

}

object EntityProtocol extends DefaultJsonProtocol with SprayJsonSupport {

  import model.AttributeProtocol._
  import model.RefDataProtocol._

  implicit val createEntityRequestFormat: RootJsonFormat[CreateEntityRequest] = jsonFormat3(CreateEntityRequest)
  implicit val entityCreationResponseFormat = jsonFormat1(EntityCreationResponse)
  implicit val entityFormat: RootJsonFormat[Entity] = jsonFormat7(Entity)
}