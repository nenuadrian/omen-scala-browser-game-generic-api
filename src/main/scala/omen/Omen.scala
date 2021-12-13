package omen

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import core.{Engine, H2Database, OmenConfigValidator, TimeProvider}
import model.Entity
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.logging.log4j.scala.Logging

import scala.util.Properties

object Omen extends App with Logging with H2Database {
  val configsPath = Properties.envOrNone("config_path")
  val port = Properties.envOrElse("PORT", 8083.toString).toInt

  implicit val system: ActorSystem = ActorSystem("web-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val ds = generateDataSource
  refresh(ds)

  def leaderboardAgent(player: Entity, entities: List[Entity]): List[(String, Int)] = {

    List(("main", entities.count(_.id == "planets")))
  }

  val engine = new Engine(configsPath match {
    case Some(path) => OmenConfigValidator.parse(path)
    case _ => OmenConfigValidator.parse(ClassLoader
      .getSystemResourceAsStream("game_configs/space.yaml"))
  }, leaderboardAgent)(ds, new TimeProvider())

  private val bindingFuture = Http().bindAndHandle(engine.webRoutes.route, "localhost", port)


  logger.info(s"OMEN Engine is now ONLINE on port $port!")


  private def mysql = {
    val clientConnPool = new BasicDataSource()
    val jdbcSettings = "?&autoReconnect=true&failOverReadOnly=false&maxReconnects=10"
    val urlClient = "jdbc:mysql://localhost" + jdbcSettings
    val driver = "com.mysql.jdbc.Driver"
    clientConnPool.setDriverClassName(driver)
    clientConnPool.setUrl(urlClient)
    clientConnPool.setInitialSize(2)
    clientConnPool.setUsername("root")
    clientConnPool.setPassword("root")
    clientConnPool
  }
}
