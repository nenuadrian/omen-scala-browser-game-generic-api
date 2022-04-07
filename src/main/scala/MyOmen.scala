import core.Omen
import model.Entity

object MyOmen extends App with Omen {
  def leaderboardAgent(player: Entity, entities: List[Entity]): List[(String, Int)] = {
    List(("main", entities.count(_.id == "planets")))
  }

  start()
}
