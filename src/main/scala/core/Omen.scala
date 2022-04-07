package core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import core.base.{EngineBase, StorageEngine}
import core.impl.Engine
import core.util.{OmenConfigValidator, TimeProvider}
import model.{EngineConfig, Entity}
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.logging.log4j.scala.Logging

import scala.util.Properties

trait Omen extends Logging {
  def leaderboardAgent(player: Entity, entities: List[Entity]): List[(String, Int)]
  protected def storageEngine(config: EngineConfig): StorageEngine

  private def engine: EngineBase = {
    val configsPath = Properties.envOrNone("config_path")
    val config = configsPath match {
      case Some(path) => OmenConfigValidator.parse(path)
      case _ => OmenConfigValidator.parse(ClassLoader
        .getSystemResourceAsStream("game_configs/space.yaml"))
    }
    new Engine(config, leaderboardAgent)(storageEngine(config), new TimeProvider())
  }

  def start(): Unit = {
    val port = Properties.envOrElse("PORT", 8083.toString).toInt
    implicit val system: ActorSystem = ActorSystem("web-server")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val bindingFuture = Http().bindAndHandle(engine.webRoutes.route, "0.0.0.0", port)
    logger.info(s"OMEN Engine is now ONLINE on port $port!")
  }
}
