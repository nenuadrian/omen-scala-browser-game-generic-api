package model

import com.github.t3hnar.bcrypt._
import core.util.TimeProvider
import spray.json._

import java.sql.{Connection, ResultSet}


case class Player(playerId: String) extends DBEntity[Player] {
  override def save()(implicit conn: Connection): Player = {
    this
  }

  override def put()(implicit conn: Connection, timeProvider: TimeProvider): Player = {
    val statement = conn.prepareStatement(s"insert into player (player_id) values(?, ?, ?)")
    statement.setString(1, playerId)
    statement.execute()
    this
  }
}

object PlayerProtocol extends DefaultJsonProtocol {
  implicit val playerFormat: RootJsonFormat[Player] = jsonFormat1(Player)

}