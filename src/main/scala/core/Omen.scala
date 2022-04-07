package core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import core.impl.EngineH2
import core.storage.H2Database
import core.util.{OmenConfigValidator, TimeProvider}
import model.Entity
import org.apache.logging.log4j.scala.Logging

import scala.util.Properties

trait Omen extends Logging with H2Database {
  def leaderboardAgent(player: Entity, entities: List[Entity]): List[(String, Int)]

  def start(): Unit = {
    val configsPath = Properties.envOrNone("config_path")
    val port = Properties.envOrElse("PORT", 8083.toString).toInt

    implicit val system: ActorSystem = ActorSystem("web-server")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val ds = generateDataSource
    refresh(ds)

    val engine = new EngineH2(configsPath match {
      case Some(path) => OmenConfigValidator.parse(path)
      case _ => OmenConfigValidator.parse(ClassLoader
        .getSystemResourceAsStream("game_configs/space.yaml"))
    }, leaderboardAgent)(ds, new TimeProvider())

    val bindingFuture = Http().bindAndHandle(engine.webRoutes.route, "0.0.0.0", port)

    logger.info(s"OMEN Engine is now ONLINE on port $port!")
  }
}
