import core.Omen
import core.base.EngineBase
import core.util.{OmenConfigValidator, TimeProvider}
import impl.EngineH2
import model.Entity
import storage.H2Database

import scala.util.Properties

object MyOmen extends App with Omen with H2Database {
  def leaderboardAgent(player: Entity, entities: List[Entity]): List[(String, Int)] = {
    List(("main", entities.count(_.id == "planets")))
  }

  override def engine: EngineBase = {
    val configsPath = Properties.envOrNone("config_path")
    val ds = generateDataSource
    refresh(ds)

    new EngineH2(configsPath match {
      case Some(path) => OmenConfigValidator.parse(path)
      case _ => OmenConfigValidator.parse(ClassLoader
        .getSystemResourceAsStream("game_configs/space.yaml"))
    }, leaderboardAgent)(ds, new TimeProvider())
  }

  start()
}
