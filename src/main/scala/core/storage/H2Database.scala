package core.storage

import org.apache.commons.dbcp2.BasicDataSource

import scala.util.{Properties, Random}

trait H2Database {

  protected def generateDataSource: BasicDataSource = {
    val clientConnPool = new BasicDataSource()
    clientConnPool.setDriverClassName("org.h2.Driver")
    clientConnPool.setUrl("jdbc:h2:~/test" + Random.nextInt())
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
}
