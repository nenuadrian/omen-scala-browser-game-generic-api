package model

import model.CustomYaml.{yamlFormat2, yamlFormat5, yamlFormat7}
import net.jcazevedo.moultingyaml.DefaultYamlProtocol

object CustomYaml extends DefaultYamlProtocol {
  implicit val attributeConfigFormat = yamlFormat5(AttributeConfig)
  implicit val entityRequirementsFormat = yamlFormat2(EntityRequirements)
  implicit val entityConfigFormat = yamlFormat7(EntityConfig)
  implicit val engineConfigFormat = yamlFormat2(EngineConfig)
}
