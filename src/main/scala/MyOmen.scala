import core.Omen
import core.base.StorageEngine
import core.util.TimeProvider
import model.{EngineConfig, Entity}
import storage.H2Database

object MyOmen extends App with Omen {
  def leaderboardAgent(player: Entity, entities: List[Entity]): List[(String, Int)] = {
    List(("main", entities.count(_.id == "planets")))
  }
  protected def storageEngine(config: EngineConfig): StorageEngine = new H2Database()(timeProvider = new TimeProvider(), config = config)
  start()
}
