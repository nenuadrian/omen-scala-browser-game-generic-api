package model

import spray.json._

case class CreateTaskRequest(duration: Int, data: Option[JsValue] = None)

case class Task(task_id: String, entity_id: String, duration: Int, endTimestamp: Long, acknowledged: Boolean = false,
                finished: Boolean = false, data: JsValue) {

  def ack(ack: Boolean = true): Task = this.copy(acknowledged = ack)
}


object TaskProtocol extends DefaultJsonProtocol {
  implicit val taskFormat: RootJsonFormat[Task] = jsonFormat7(Task)
  implicit val createTaskRequestFormat: RootJsonFormat[CreateTaskRequest] = jsonFormat2(CreateTaskRequest)
}