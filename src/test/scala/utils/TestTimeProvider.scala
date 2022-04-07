package utils

import core.util.TimeProvider

class TestTimeProvider extends TimeProvider {
  var currentTimestampOverride: Option[Long] = None

  override def currentTimestamp: Long = currentTimestampOverride match {
    case Some(ts) => ts
    case _ => super.currentTimestamp
  }
}