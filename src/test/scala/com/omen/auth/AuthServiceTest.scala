package com.omen.auth

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.omen.auth.core.JsonSupport._
import com.omen.auth.core._
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import spray.json.DefaultJsonProtocol

class AuthServiceTest extends WordSpec with BeforeAndAfter with H2Database with Matchers with ScalatestRouteTest with DefaultJsonProtocol {
  var engine: Engine = _


  before {
    val ds = generateDataSource
    refresh(ds)
    engine = new Engine(ds)
  }

  "The engine" should {
    "ping" in {
      Get(s"/ping") ~> engine.webRoutes.route ~> check {
        handled shouldBe true
      }
    }

    "login failure" in {
      Post(s"/login/player/1.1.1.1", LoginRequest("test@test.com", "test")) ~> engine.webRoutes.route ~> check {
        handled shouldBe true
        val result = responseAs[LoginResult]
        result.status shouldBe "AccessDeniedException"
      }
    }

    "signup" in {
      Post(s"/signup/player", SignupRequest("test@test.com", "test")) ~> engine.webRoutes.route ~> check {
        handled shouldBe true
        val result = responseAs[SignupResponse]
        Post(s"/login/player/1.1.1.1", LoginRequest("test@test.com", "test")) ~> engine.webRoutes.route ~> check {
          handled shouldBe true
          val result = responseAs[LoginResult]
          result.status shouldBe "success"
        }
      }
    }
  }

}
