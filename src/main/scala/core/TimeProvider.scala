package core

import java.util.Calendar

class TimeProvider {
  def currentTimestamp: Long = Calendar.getInstance.getTime.getTime
}
