import akka.http.scaladsl.testkit.ScalatestRouteTest
import core.impl.Engine
import core.util.OmenConfigValidator
import model.EngineConfig
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import spray.json.DefaultJsonProtocol
import storage.H2Database
import utils.TestTimeProvider

class TestBed(id: String) extends WordSpec with BeforeAndAfter  with Matchers with ScalatestRouteTest with DefaultJsonProtocol {
  private val inputStream = getClass.getClassLoader.getResourceAsStream(s"game_configs/$id.yaml")
  private val engineConfig: EngineConfig = OmenConfigValidator.parse(inputStream)
  var engine: Engine = _
  var timeProvider: TestTimeProvider = _

  before {
    timeProvider = new TestTimeProvider
    engine = new Engine(engineConfig, (e, ee) => List(("main", 1)), false)(new H2Database()(timeProvider, engineConfig), timeProvider)
  }

  after {

  }
}
