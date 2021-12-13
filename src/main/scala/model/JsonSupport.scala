package model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import api.{EntitiesQuery, RefDataQuery}
import model.JsonSupport.{jsonFormat2, jsonFormat3, jsonFormat4, jsonFormat7}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  import model.AttributeProtocol._

  implicit val RefDataQueryFormat: RootJsonFormat[RefDataQuery] = jsonFormat2(RefDataQuery)
  implicit val entitiesQueryFormat: RootJsonFormat[EntitiesQuery] = jsonFormat5(EntitiesQuery)
  implicit val entityRequirementsFormat: RootJsonFormat[EntityRequirements] = jsonFormat2(EntityRequirements)

  implicit val entityConfigFormat: RootJsonFormat[EntityConfig] = jsonFormat7(EntityConfig)

  implicit val engineConfigFormat: RootJsonFormat[EngineConfig] = jsonFormat2(EngineConfig)
  implicit val requirementResponseFormat: RootJsonFormat[RequirementResponse] = jsonFormat3(RequirementResponse)
  implicit val requirementsResponseFormat: RootJsonFormat[RequirementsResponse] = jsonFormat4(RequirementsResponse)
}
