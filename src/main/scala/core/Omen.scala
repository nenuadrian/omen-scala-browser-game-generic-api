package core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import core.base.EngineBase
import core.util.{OmenConfigValidator, TimeProvider}
import impl.EngineH2
import model.Entity
import org.apache.logging.log4j.scala.Logging
import storage.H2Database

import scala.util.Properties

trait Omen extends Logging {
  def leaderboardAgent(player: Entity, entities: List[Entity]): List[(String, Int)]
  def engine: EngineBase

  def start(): Unit = {
    val port = Properties.envOrElse("PORT", 8083.toString).toInt
    implicit val system: ActorSystem = ActorSystem("web-server")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val bindingFuture = Http().bindAndHandle(engine.webRoutes.route, "0.0.0.0", port)
    logger.info(s"OMEN Engine is now ONLINE on port $port!")
  }
}
