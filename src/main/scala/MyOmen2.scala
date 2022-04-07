import core.Omen
import core.base.StorageEngine
import core.util.TimeProvider
import model.{EngineConfig, Entity}
import storage.NoSQLDatabase

object MyOmen2 extends App with Omen {
  def leaderboardAgent(player: Entity, entities: List[Entity]): List[(String, Int)] = {
    List(("main", entities.count(_.id == "planets")))
  }
  protected def storageEngine(config: EngineConfig): StorageEngine = new NoSQLDatabase()(timeProvider = new TimeProvider(), config = config)
  start()
}
