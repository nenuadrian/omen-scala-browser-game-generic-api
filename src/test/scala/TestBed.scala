import akka.http.scaladsl.testkit.ScalatestRouteTest
import core.{Engine, H2Database, OmenConfigValidator}
import model.EngineConfig
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import spray.json.DefaultJsonProtocol
import utils.TestTimeProvider

class TestBed(id: String) extends WordSpec with BeforeAndAfter with H2Database with Matchers with ScalatestRouteTest with DefaultJsonProtocol {
  private val inputStream = getClass.getClassLoader.getResourceAsStream(s"game_configs/$id.yaml")
  private val engineConfig: EngineConfig = OmenConfigValidator.parse(inputStream)
  var engine: Engine = _
  var timeProvider: TestTimeProvider = _

  before {
    val ds = generateDataSource
    refresh(ds)
    timeProvider = new TestTimeProvider
    engine = new Engine(engineConfig, (e, ee) => List(("main", 1)), false)(ds, timeProvider)
  }

  after {

  }
}