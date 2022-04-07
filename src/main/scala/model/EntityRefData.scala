package model

import core.util.TimeProvider
import spray.json._

import java.sql.{Connection, ResultSet}


case class RefData(entity_id: String, ref_key: String, ref_value: String) extends DBEntity[RefData] {
  def save()(implicit conn: Connection): RefData = {
    val statement = conn.prepareStatement(s"update entity_ref_data set ref_value = ? where entity_id = ? and ref_key = ?")
    statement.setString(2, entity_id)
    statement.setString(3, ref_key)
    statement.setString(1, ref_value)
    statement.executeUpdate()
    this
  }

  def put()(implicit conn: Connection, timeProvider: TimeProvider): RefData = {
    val statement = conn.prepareStatement(s"insert into entity_ref_data (entity_id, ref_key, ref_value) values(?, ?, ?)")
    statement.setString(1, entity_id)
    statement.setString(2, ref_key)
    statement.setString(3, ref_value)
    statement.executeUpdate()
    this
  }

}


object RefDataProtocol extends DefaultJsonProtocol {
  implicit val refDataFormat: RootJsonFormat[RefData] = jsonFormat3(RefData)

  implicit def rs2refData(rs: ResultSet): RefData =
    RefData(rs.getString("entity_id"), rs.getString("ref_key"), rs.getString("ref_value"))
}