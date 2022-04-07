package storage

import core.base.StorageEngine
import core.util.TimeProvider
import model.{Attribute, EngineConfig, Entity, RefData, Task}
import org.apache.commons.dbcp2.BasicDataSource

import java.sql.{Connection, PreparedStatement, ResultSet}
import java.util.Date
import scala.language.implicitConversions
import scala.util.{Properties, Random}
import spray.json._

class H2Database(implicit timeProvider: TimeProvider, config: EngineConfig) extends StorageEngine {
  private implicit def rs2Attribute(rs: ResultSet): Attribute =
    Attribute(rs.getString("name"), rs.getString("entity_id"), Some(rs.getString("value")),
      rs.getLong("last_hourly_timestamp"))


  private implicit def rs2Entity(rs: ResultSet): Entity =
    Entity(rs.getString("entity_id"), rs.getString("id"),
      List(), List(), Option(rs.getString("primary_parent_entity_id")),
      Option(rs.getString("parent_entity_id")), rs.getInt("amount"))


  private implicit def rs2refData(rs: ResultSet): RefData =
    RefData(rs.getString("entity_id"), rs.getString("ref_key"), rs.getString("ref_value"))


  private implicit def rs2task(rs: ResultSet): Task =
    Task(rs.getString("task_id"), rs.getString("entity_id"), rs.getInt("duration"),
      rs.getLong("end_timestamp"),
      acknowledged = rs.getString("acknowledged") == "1", finished = rs.getString("finished") == "1",
      data = rs.getString("data").parseJson)

  protected def generateDataSource: BasicDataSource = {
    val clientConnPool = new BasicDataSource()
    clientConnPool.setDriverClassName("org.h2.Driver")
    clientConnPool.setUrl("jdbc:h2:~/test" + new Date().getTime)
    clientConnPool.setInitialSize(2)
    clientConnPool.setUsername("sa")
    clientConnPool.setPassword("")
    clientConnPool
  }

  protected val db: BasicDataSource = generateDataSource
  refresh(db)

  def refresh(clientConnPool: BasicDataSource): Unit = {
    val conn = clientConnPool.getConnection
    val creates = List(
      "CREATE TABLE IF NOT EXISTS attributes ( entity_id VARCHAR(256) NOT NULL, name VARCHAR(256) NOT NULL, value VARCHAR(256) NOT NULL, last_hourly_timestamp BIGINT(20) default null );",
      "CREATE TABLE IF NOT EXISTS entities ( entity_id VARCHAR(256) NOT NULL, id VARCHAR(256) NOT NULL, primary_parent_entity_id VARCHAR(256), parent_entity_id varchar(256), amount INT(21) not null);",
      "CREATE TABLE IF NOT EXISTS entity_ref_data ( entity_id VARCHAR(256) NOT NULL, ref_key VARCHAR(256) NOT NULL, ref_value VARCHAR(256) NOT NULL);",
      "CREATE TABLE IF NOT EXISTS task ( task_id VARCHAR(256) NOT NULL, entity_id VARCHAR(256) NOT NULL, duration int(11), end_timestamp  BIGINT(20), acknowledged tinyint(1) default 0, finished tinyint(1) default null, data VARCHAR(1000));",
      "CREATE TABLE IF NOT EXISTS reward ( reward_id VARCHAR(256)  NOT NULL, player_id varchar(256) not null, entity_id VARCHAR(256) NOT NULL, claimed int(1) default null);",
      "CREATE TABLE IF NOT EXISTS reward_content ( reward_id VARCHAR(256)  NOT NULL, entity_id VARCHAR(256) default NULL, attribute_id varchar(256) default null, amount bigint(20));"
    )
    if (Properties.envOrElse("db_refresh", "true").toLowerCase != "false") {
      creates.map(c => c.split(" ")(2)).foreach(table => conn.prepareStatement(s"DROP TABLE IF EXISTS $table").execute())
    }
    creates.foreach(c => conn.prepareStatement(c).execute())
    conn.close()
  }

  def withDb[T](method: Connection => T): T = {
    val conn = db.getConnection
    try {
      method(conn)
    } finally {
      conn.close()
    }
  }

  override def updateFinishedTasks(): Unit = {
    withDb { conn =>
      val statement = conn.prepareStatement(s"update task set finished = 1 where end_timestamp <= ?")
      statement.setLong(1, timeProvider.currentTimestamp)
      statement.execute()
    }
  }

  override def tasksWithPlayerId(playerId: Option[String], entityId: Option[String], acknowledged: Boolean = false): List[Task] = {
    withDb { conn =>
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
        case _ => throw new UnsupportedOperationException
      }
      statement.setInt(2, if (acknowledged) 1 else 0)
      preparedStatement2List(statement)
    }
  }

  override def save[T](e: T): T = {
    withDb { implicit conn =>
      e match {
        case e: Attribute => {
          val statement = conn.prepareStatement(s"update attributes set value = ?, last_hourly_timestamp = ? where entity_id = ? and name = ?")
          statement.setString(1, e.value.getOrElse(0).toString)
          statement.setLong(2, e.lastHourlyTimestamp)
          statement.setString(3, e.entity_id)
          statement.setString(4, e.attr)
          statement.executeUpdate()
        }
        case e: Entity => {
            val statement = conn.prepareStatement(s"update `entities` set amount = ? WHERE entity_id = ?")
            statement.setInt(1, e.amount)
            statement.setString(2, e.entity_id)
            statement.execute()
        }
        case e: RefData => {
            val statement = conn.prepareStatement(s"update entity_ref_data set ref_value = ? where entity_id = ? and ref_key = ?")
            statement.setString(2, e.entity_id)
            statement.setString(3, e.ref_key)
            statement.setString(1, e.ref_value)
            statement.executeUpdate()

        }
        case e: Task => {
            val statement = conn.prepareStatement(s"update task set acknowledged = ?, data = ? where task_id = ?")
            statement.setInt(1, if (e.acknowledged) 1 else 0)
            statement.setString(2, e.data.compactPrint)
            statement.setString(3, e.task_id)
            statement.execute()
        }
        case _ => throw new UnsupportedOperationException
      }
    }

    e
  }
  override def put[T](e: T): T = {
    withDb { implicit conn =>
      e match {
        case e: Attribute => {
          val statement = conn.prepareStatement(s"insert into attributes (entity_id, name, value, last_hourly_timestamp) values(?, ?, ?, ?)")
          statement.setString(1, e.entity_id)
          statement.setString(2, e.attr)
          statement.setString(3, e.value.getOrElse(0).toString)
          statement.setLong(4, timeProvider.currentTimestamp)
          statement.executeUpdate()
        }
        case e: Entity => {
            val statement = conn.prepareStatement(s"insert into entities (entity_id, id, primary_parent_entity_id, parent_entity_id, amount) values(?, ?, ?, ?, ?)")
            statement.setString(1, e.entity_id)
            statement.setString(2, e.id)
            statement.setString(3, e.primary_parent_entity_id.orNull)
            statement.setString(4, e.parent_entity_id.orNull)
            statement.setInt(5, e.amount)
            statement.execute()
        }
        case e: RefData => {
          val statement = conn.prepareStatement(s"insert into entity_ref_data (entity_id, ref_key, ref_value) values(?, ?, ?)")
          statement.setString(1, e.entity_id)
          statement.setString(2, e.ref_key)
          statement.setString(3, e.ref_value)
          statement.executeUpdate()
        }
        case e: Task => {
            val statement = conn.prepareStatement(s"insert into task (task_id, entity_id, duration, end_timestamp, data) values(?, ?, ?, ?, ?)")
            statement.setString(1, e.task_id)
            statement.setString(2, e.entity_id)
            statement.setInt(3, e.duration)
            statement.setLong(4, e.endTimestamp)
            statement.setString(5, e.data.compactPrint)
            statement.execute()
        }
        case _ => throw new UnsupportedOperationException
      }
    }
    e
  }

  override def entities(ids: List[String]): List[Entity] = {
    withDb { implicit conn =>
      val statement = conn.prepareStatement(s"SELECT * FROM `entities` WHERE id in ('" + ids.mkString("', '") + "')")
      extractEntitiesFromRs(preparedStatement2List(statement))
    }
  }

  def extractEntitiesFromRs(entities: List[Entity])(implicit conn: Connection): List[Entity] = {
    val allAttributes = attributesForEntities(entities.map(_.entity_id))
    val allRefData = refdataForEntities(entities.map(_.entity_id))
    entities.map(entity => {
      val entityDescription = config.entityConfigById(entity.id).get
      val attributes = allAttributes.getOrElse(entity.entity_id, List())
      entity.copy(
        attributes = attributes ++ entityDescription.have.getOrElse(List())
          .filter(a => !attributes.exists(p => p.attr == a.id))
          .map(a => Attribute(a.id, entity.entity_id, Some(a.default.getOrElse(0).toString), timeProvider.currentTimestamp)),
        refData = allRefData.getOrElse(entity.entity_id, List()))
    })
  }

  implicit def preparedStatement2List[T](statement: PreparedStatement)(implicit rs2t: ResultSet => T): List[T] = {
    val rs = statement.executeQuery
    var attributes: List[T] = List()
    while (rs.next()) {
      val attr: T = rs
      attributes = attributes :+ attr
    }
    attributes
  }

  implicit def preparedStatement2Entity[T](statement: PreparedStatement)(implicit rs2t: ResultSet => T): Option[T] = {
    val rs = statement.executeQuery
    if (rs.next()) {
      Some(rs)
    } else None
  }

  override def attributesForEntities(ids: List[String]): Map[String, List[Attribute]] = {
    withDb { implicit conn =>
      val statement = conn.prepareStatement(s"SELECT * FROM `attributes` WHERE entity_id in ('" + ids.mkString("', '") + "')")
      preparedStatement2List[Attribute](statement).groupBy(a => a.entity_id)
    }
  }

  def refdataForEntities(ids: List[String]): Map[String, List[RefData]] = {
    withDb { implicit conn =>
      val statement = conn.prepareStatement(s"SELECT * FROM `entity_ref_data` WHERE entity_id in ('" + ids.mkString("', '") + "')")
      preparedStatement2List[RefData](statement).groupBy(a => a.entity_id)
    }
  }

  override def entitiesWithPlayerId(primaryParentEntityId: Option[String], parent_entity_id: Option[String]): List[Entity] = {
    withDb { implicit conn =>
      val statement = (primaryParentEntityId, parent_entity_id) match {
        case (None, None) => {
          val statement = conn.prepareStatement(s"SELECT * FROM `entities`")
          statement
        }
        case (Some(id), None) => {
          val statement = conn.prepareStatement(s"SELECT * FROM `entities` WHERE primary_parent_entity_id = ?")
          statement.setString(1, id)
          statement
        }
        case (None, Some(id)) => {
          val statement = conn.prepareStatement(s"SELECT * FROM `entities` WHERE parent_entity_id = ?")
          statement.setString(1, id)
          statement
        }
        case (Some(pid), Some(id)) => {
          val statement = conn.prepareStatement(s"SELECT * FROM `entities` WHERE primary_parent_entity_id = ? and parent_entity_id = ?")
          statement.setString(1, pid)
          statement.setString(2, id)
          statement
        }
        case _ => throw new UnsupportedOperationException
      }
      extractEntitiesFromRs(preparedStatement2List(statement))
    }
  }

  override def tasks(ids: List[String] = List(), entities: List[String] = List()): List[Task] = {
    withDb { conn =>
      val statement = conn.prepareStatement(s"SELECT * FROM `task` WHERE " +
        List((if (ids.nonEmpty) Some("task_id in ('" + ids.mkString("', '") + "')") else None),
          (if (entities.nonEmpty) Some("entity_id in ('" + entities.mkString("', '") + "')") else None)).flatten.mkString(" and ")
      )
      preparedStatement2List(statement)
    }
  }
}
