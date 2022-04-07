package model

import core.util.TimeProvider
import spray.json._

import java.sql.{Connection, ResultSet}

case class AttributePair(attribute: AttributeDescription)

case class AttributeDescription(name: String)

case class Attribute(attr: String, entity_id: String, value: Option[String], lastHourlyTimestamp: Long) extends DBEntity[Attribute] {
  def save()(implicit conn: Connection): Attribute = {
    val statement = conn.prepareStatement(s"update attributes set value = ?, last_hourly_timestamp = ? where entity_id = ? and name = ?")
    statement.setString(1, value.getOrElse(0).toString)
    statement.setLong(2, lastHourlyTimestamp)
    statement.setString(3, entity_id)
    statement.setString(4, attr)
    statement.executeUpdate()
    this
  }

  def put()(implicit conn: Connection, timeProvider: TimeProvider): Attribute = {
    val statement = conn.prepareStatement(s"insert into attributes (entity_id, name, value, last_hourly_timestamp) values(?, ?, ?, ?)")
    statement.setString(1, entity_id)
    statement.setString(2, attr)
    statement.setString(3, value.getOrElse(0).toString)
    statement.setLong(4, timeProvider.currentTimestamp)
    statement.executeUpdate()
    this
  }

}

object AttributeProtocol extends DefaultJsonProtocol {
  implicit val attributeDescriptionFormat = jsonFormat1(AttributeDescription)
  implicit val attributePairFormat = jsonFormat1(AttributePair)
  implicit val attributeFormat: RootJsonFormat[Attribute] = jsonFormat4(Attribute)

  implicit val attributeConfigFormat = jsonFormat5(AttributeConfig)

  implicit def rs2Attribute(rs: ResultSet): Attribute =
    Attribute(rs.getString("name"), rs.getString("entity_id"), Some(rs.getString("value")),
      rs.getLong("last_hourly_timestamp"))
}