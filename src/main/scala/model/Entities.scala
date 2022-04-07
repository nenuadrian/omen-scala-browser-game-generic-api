package model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import core.util.TimeProvider
import net.jcazevedo.moultingyaml.DefaultYamlProtocol
import org.apache.commons.dbcp2.BasicDataSource
import spray.json._

import java.sql.{Connection, PreparedStatement, ResultSet}
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

trait DBEntity[T] {
  def save()(implicit conn: Connection): T

  def put()(implicit conn: Connection, timeProvider: TimeProvider): T
}


object EngineConfigUtils {
  implicit def preparedStatement2List[T](statement: PreparedStatement)(implicit rs2t: ResultSet => T): List[T] = {
    val rs = statement.executeQuery
    var attributes: List[T] = List()
    while (rs.next()) {
      val attr: T = rs
      attributes = attributes :+ attr
    }
    attributes
  }

  implicit def preparedStatement2Entity[T](statement: PreparedStatement)(implicit rs2t: ResultSet => T): Option[T] = {
    val rs = statement.executeQuery
    if (rs.next()) {
      Some(rs)
    } else None
  }

  def withDb[T](method: Connection => T)(implicit db: BasicDataSource): T = {
    val conn = db.getConnection
    try {
      method(conn)
    } finally {
      conn.close()
    }
  }
}