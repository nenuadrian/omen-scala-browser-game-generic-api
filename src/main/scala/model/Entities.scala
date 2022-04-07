package model

import spray.json._

import scala.language.implicitConversions


case class EntityDescription(name: String, attributes: List[AttributePair], subEntity: List[EntityDescription] = Nil)

case class RequirementResponse(current: String, requirement: AttributeConfig, fulfilled: Boolean)

case class RequirementsResponse(attributes: List[RequirementResponse], entities: List[RequirementResponse], have: List[RequirementResponse], fulfilled: Boolean)


case class EngineConfig(name: String,
                        entities: List[EntityConfig] = List()) {
  def toGraph: JsValue = {
    val mapping = entities.zipWithIndex.map(e => e._1.id -> e._2).toMap
    JsObject(
      "entities" -> JsArray(
        entities.zipWithIndex.map(e => JsObject(
          "id" -> JsNumber(e._2),
          "label" -> JsString(e._1.id),
        )).toVector
      ),
      "edges" -> JsArray(
        entities.flatMap(e => {
          (
            e.own.map(req => req.map(own => JsObject(
              "type" -> JsString("owns"),
              "label" -> JsString("owns"),
              "from" -> JsNumber(mapping(e.id)),
              "to" -> JsNumber(mapping(own))
            ))).getOrElse(List()) ++ e.requirements.flatMap(req => req.entities.map(req => {
              req.map(r => JsObject(
                "label" -> JsString("requirement"),
                "type" -> JsString("requirement"),
                "from" -> JsNumber(mapping(e.id)),
                "to" -> JsNumber(mapping(r.id.replace("parent[", "").replace("]", "")))
              ))
            })).getOrElse(List())).toVector
        }).toVector
      )
    )
  }

  def entityConfigById(id: String): Option[EntityConfig] = entities.find(_.id == id)
}

case class EntityRequirements(entities: Option[List[AttributeConfig]], attributes: Option[List[AttributeConfig]])

case class EntityConfig(id: String,
                        tag: Option[String],
                        upgradeable: Option[Boolean],
                        aggregateable: Option[Boolean] = Some(false),
                        own: Option[List[String]],
                        have: Option[List[AttributeConfig]],
                        requirements: Option[EntityRequirements])

case class AttributeConfig(id: String, default: Option[Int], max: Option[Int], formula: Option[String],
                           hourly_rate_attribute: Option[String])



