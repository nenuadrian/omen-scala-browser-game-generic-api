package model

import spray.json._


case class RefData(entity_id: String, ref_key: String, ref_value: String)

object RefDataProtocol extends DefaultJsonProtocol {
  implicit val refDataFormat: RootJsonFormat[RefData] = jsonFormat3(RefData)
}