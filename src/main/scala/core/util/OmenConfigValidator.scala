package core.util

import model.EngineConfig
import org.apache.commons.io.IOUtils
import net.jcazevedo.moultingyaml._
import model.CustomYaml._
import java.io.{FileInputStream, InputStream}
import java.nio.charset.StandardCharsets

object OmenConfigValidator {

  def parse(configsPath: String): EngineConfig = {
    parse(new FileInputStream(configsPath))
  }

  def parse(is: InputStream): EngineConfig = {
    val result: String = IOUtils.toString(is, StandardCharsets.UTF_8)
    validate(result.parseYaml.convertTo[EngineConfig])
  }

  private def validate(config: EngineConfig): EngineConfig = {
    if (!config.entities.exists(_.id == "players")) {
      throw new Exception("Missing expected Players entity id")
    }

    config.entities.foreach(ec => ec.own.map(_.foreach(o =>
      if (!config.entities.exists(_.id == o)) throw new Exception(s"Entity ${ec.id} owns $o which is not defined"))))

    config
  }
}
