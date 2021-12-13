package model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import core.TimeProvider
import model.EngineConfigUtils.preparedStatement2List
import spray.json._

import java.sql.{Connection, ResultSet}
import java.util.UUID.randomUUID

case class CreateEntityRequest(id: String, entity_parent: Option[String], entity_primary_parent: Option[String])

case class EntityCreationResponse(entity_id: String)

case class Entity(entity_id: String = randomUUID.toString, id: String, attributes: List[Attribute] = List(),
                  refData: List[RefData] = List(), primary_parent_entity_id: Option[String], parent_entity_id: Option[String], amount: Int)
  extends DBEntity[Entity] {

  import model.AttributeProtocol._

  def put()(implicit conn: Connection, timeProvider: TimeProvider): Entity = {
    val statement = conn.prepareStatement(s"insert into entities (entity_id, id, primary_parent_entity_id, parent_entity_id, amount) values(?, ?, ?, ?, ?)")
    statement.setString(1, entity_id)
    statement.setString(2, id)
    statement.setString(3, primary_parent_entity_id.orNull)
    statement.setString(4, parent_entity_id.orNull)
    statement.setInt(5, amount)
    statement.execute()
    this
  }

  def save()(implicit conn: Connection): Entity = {
    val statement = conn.prepareStatement(s"update `entities` set amount = ? WHERE entity_id = ?")
    statement.setInt(1, amount)
    statement.setString(2, entity_id)
    statement.execute()
    this
  }

  def attributes()(implicit conn: Connection): List[Attribute] = {
    val statement = conn.prepareStatement(s"SELECT * FROM `attributes` WHERE entity_id = ?")
    statement.setString(1, entity_id)
    preparedStatement2List(statement)
  }

  def updateAttribute(attribute: Attribute)(implicit conn: Connection, timeProvider: TimeProvider): Attribute = {
    attributes().find(_.attr == attribute.attr) match {
      case Some(_) => attribute.save()
      case _ => attribute.put()
    }
  }

}

object EntityProtocol extends DefaultJsonProtocol with SprayJsonSupport {

  import model.AttributeProtocol._
  import model.RefDataProtocol._

  implicit val createEntityRequestFormat: RootJsonFormat[CreateEntityRequest] = jsonFormat3(CreateEntityRequest)
  implicit val entityCreationResponseFormat = jsonFormat1(EntityCreationResponse)
  implicit val entityFormat: RootJsonFormat[Entity] = jsonFormat7(Entity)


  implicit def rs2Entity(rs: ResultSet): Entity =
    Entity(rs.getString("entity_id"), rs.getString("id"),
      List(), List(), Option(rs.getString("primary_parent_entity_id")),
      Option(rs.getString("parent_entity_id")), rs.getInt("amount"))

}