package model

import spray.json._

case class AttributePair(attribute: AttributeDescription)
case class AttributeDescription(name: String)
case class Attribute(attr: String, entity_id: String, value: Option[String], lastHourlyTimestamp: Long)

object AttributeProtocol extends DefaultJsonProtocol {
  implicit val attributeDescriptionFormat = jsonFormat1(AttributeDescription)
  implicit val attributePairFormat = jsonFormat1(AttributePair)
  implicit val attributeFormat: RootJsonFormat[Attribute] = jsonFormat4(Attribute)
  implicit val attributeConfigFormat = jsonFormat5(AttributeConfig)
}