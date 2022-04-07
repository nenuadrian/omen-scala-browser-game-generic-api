package storage

import core.base.StorageEngine
import core.util.TimeProvider
import model._
import org.apache.commons.dbcp2.BasicDataSource

import java.sql.{Connection, PreparedStatement, ResultSet}
import java.util.Date
import scala.util.Properties

class NoSQLDatabase(implicit timeProvider: TimeProvider, config: EngineConfig) extends StorageEngine {
  import model.AttributeProtocol._
  import model.EntityProtocol._
  import model.RefDataProtocol._
  import model.TaskProtocol._

  protected def generateDataSource: BasicDataSource = {
    val clientConnPool = new BasicDataSource()
    clientConnPool.setDriverClassName("org.h2.Driver")
    clientConnPool.setUrl("jdbc:h2:~/test" + new Date().getTime)
    clientConnPool.setInitialSize(2)
    clientConnPool.setUsername("sa")
    clientConnPool.setPassword("")
    clientConnPool
  }

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

  protected val db = generateDataSource
  refresh(db)

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

  override def save[T <: DBEntity[T]](e: T): T = {
    withDb { implicit conn =>
      e.save()
    }
  }
  override def put[T <: DBEntity[T]](e: T): T = {
    withDb { implicit conn =>
      e.put()
    }
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
