package model

case class RefDataQuery(key: String, value: String)
case class EntitiesQuery(byIdentifier: Option[String] = None, refDataFilters: Option[List[RefDataQuery]] = None,
                         parent_entity_id: Option[String] = None, tag: Option[String] = None,
                         primaryParentEntityId: Option[String] = None)
