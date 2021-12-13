package model

import core.TimeProvider
import model.EngineConfigUtils.preparedStatement2List
import spray.json._

import java.sql.{Connection, ResultSet}

case class CreateTaskRequest(duration: Int, data: Option[JsValue] = None)

case class Task(task_id: String, entity_id: String, duration: Int, endTimestamp: Long, acknowledged: Boolean = false,
                finished: Boolean = false, data: JsValue) extends DBEntity[Task] {

  def put()(implicit conn: Connection, timeProvider: TimeProvider): Task = {
    val statement = conn.prepareStatement(s"insert into task (task_id, entity_id, duration, end_timestamp, data) values(?, ?, ?, ?, ?)")
    statement.setString(1, task_id)
    statement.setString(2, entity_id)
    statement.setInt(3, duration)
    statement.setLong(4, endTimestamp)
    statement.setString(5, data.compactPrint)
    statement.execute()
    this
  }

  override def save()(implicit conn: Connection): Task = {
    val statement = conn.prepareStatement(s"update task set acknowledged = ?, data = ? where task_id = ?")
    statement.setInt(1, if (acknowledged) 1 else 0)
    statement.setString(2, data.compactPrint)
    statement.setString(3, task_id)
    statement.execute()
    this
  }
}

object TaskModel {

  import model.TaskProtocol._

  def tasksWithPlayerId(conn: Connection, playerId: Option[String], entityId: Option[String], acknowledged: Boolean = false): List[Task] = {
    val statement = (playerId, entityId) match {
      case (Some(id), None) => {
        val statement = conn.prepareStatement(s"SELECT * FROM `task` WHERE entity_id IN (select entity_id from entities where player_id = ?) and acknowledged = ?")
        statement.setString(1, id)
        statement
      }
      case (None, Some(id)) => {
        val statement = conn.prepareStatement(s"SELECT * FROM `task` WHERE entity_id = ? and acknowledged = ?")
        statement.setString(1, id)
        statement
      }
      case (None, None) => throw new UnsupportedOperationException
    }
    statement.setInt(2, if (acknowledged) 1 else 0)
    preparedStatement2List(statement)
  }

}

object TaskProtocol extends DefaultJsonProtocol {
  implicit val taskFormat: RootJsonFormat[Task] = jsonFormat7(Task)
  implicit val createTaskRequestFormat: RootJsonFormat[CreateTaskRequest] = jsonFormat2(CreateTaskRequest)

  implicit def rs2task(rs: ResultSet): Task =
    Task(rs.getString("task_id"), rs.getString("entity_id"), rs.getInt("duration"),
      rs.getLong("end_timestamp"),
      acknowledged = rs.getString("acknowledged") == "1", finished = rs.getString("finished") == "1",
      data = rs.getString("data").parseJson)
}