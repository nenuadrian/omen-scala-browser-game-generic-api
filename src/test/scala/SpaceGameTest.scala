
import model.json.JsonSupport._
import model._
import spray.json._

class SpaceGameTest extends TestBed("space") {

  import model.EntityProtocol._
  import model.PlayerProtocol._
  import model.TaskProtocol._

  private def r[T](implicit reader : spray.json.JsonReader[T]) =
    responseAs[JsObject].fields("data").convertTo[T]
    
  private def createPlayer(checks: (EntityCreationResponse, Entity) => Unit): Unit = {
    Put("/player") ~> engine.webRoutes.route ~> check {
      val player = r[EntityCreationResponse]
      Get(s"/entities?primaryParentEntityId=${player.entity_id}") ~> engine.webRoutes.route ~> check {
        val playerEntity = r[List[Entity]]
        playerEntity.size shouldBe 1
        checks(player, playerEntity.head)
      }
    }
  }

  "The engine" should {

    "create a player when asked" in {
      createPlayer((player, _) => {
        player.entity_id should not be empty
      })
    }

    "return attributes of a player when asked" in {
      createPlayer((_, pe) => {
        pe.entity_id should not be empty
        pe.attributes.find(_.attr == "dark_matter").flatMap(_.value) should be(Some("10"))
      })
    }

    "create an entity and aggregate only when needed" in {
      createPlayer((player, pe) => {
        Put(s"/entities", CreateEntityRequest("planets", Some(pe.entity_id), Some(player.entity_id))) ~> engine.webRoutes.route ~> check {
          val planetResponse = r[EntityCreationResponse]
          Put(s"/entities", CreateEntityRequest("metal-mine", Some(planetResponse.entity_id), Some(player.entity_id))) ~> engine.webRoutes.route ~> check {
            val response2 = r[EntityCreationResponse]
            Get(s"/entities?primaryParentEntityId=${player.entity_id}") ~> engine.webRoutes.route ~> check {
              val response = r[Seq[Entity]]
              response.map(_.id).toSet shouldEqual Set("espionage", "players", "shipyard", "cargo-carrier", "planets", "metal-mine", "death-star", "crystal-mine", "shipmanship", "small-fighter")
            }

            Put(s"/entities", CreateEntityRequest("small-fighter", Some(planetResponse.entity_id), Some(player.entity_id))) ~> engine.webRoutes.route ~> check {
              val response2 = r[EntityCreationResponse]
              Get(s"/entities?primaryParentEntityId=${player.entity_id}") ~> engine.webRoutes.route ~> check {
                val response = r[Seq[Entity]]
                response.size shouldEqual 11
                response.last.amount shouldEqual 1
                Post(s"/entities", EntitiesQuery(parent_entity_id = Some(planetResponse.entity_id), primaryParentEntityId = Some(player.entity_id))) ~> engine.webRoutes.route ~> check {
                  val responseWithParentFilter = r[Seq[Entity]]
                  responseWithParentFilter.size shouldEqual 9
                  Put(s"/entities?primaryParentEntityId=${player.entity_id}", CreateEntityRequest("small-fighter", Some(planetResponse.entity_id), Some(player.entity_id))) ~> engine.webRoutes.route ~> check {
                    val response2 = r[EntityCreationResponse]
                    Get(s"/entities?primaryParentEntityId=${player.entity_id}") ~> engine.webRoutes.route ~> check {
                      val response = r[Seq[Entity]]
                      response.size shouldEqual 11
                      response.last.amount shouldEqual 2
                    }
                  }
                }
              }
            }
          }
        }
      })
    }

    "ref data" in {
      createPlayer((player, pe) => {
        Put(s"/entities", CreateEntityRequest("planets", Some(pe.entity_id), Some(player.entity_id))) ~> engine.webRoutes.route ~> check {
          val planetResponse = r[EntityCreationResponse]
          Post(s"/entities/${planetResponse.entity_id}/ref/test-k/test-v?playerId=${player.entity_id}") ~> engine.webRoutes.route ~> check {
            //val response = r[String]
            Get(s"/entities/${planetResponse.entity_id}") ~> engine.webRoutes.route ~> check {
              val response = r[Entity]
              response.refData.toSet shouldEqual Set(RefData(planetResponse.entity_id, "test-k", "test-v"))
            }
          }
        }
      })
    }

    "be able to create/update attributes" in {
      createPlayer((player, pe) => {
        Post(s"/entities/${pe.entity_id}/attributes/dark_matter/500") ~> engine.webRoutes.route ~> check {
          handled shouldBe true
          Get(s"/entities?primaryParentEntityId=${player.entity_id}") ~> engine.webRoutes.route ~> check {
            val entitiesAfter = r[Seq[Entity]]
            entitiesAfter.head.attributes.find(_.attr == "dark_matter").get.value.orNull shouldEqual "500"
          }
        }
      })
    }

    "allow creation of a task against an entity" in {
      createPlayer((player, pe) => {
        Put(s"/tasks?parent_entity_id=${pe.entity_id}", CreateTaskRequest(1)) ~> engine.webRoutes.route ~> check {
          handled shouldBe true
          val task = r[Task]
          timeProvider.currentTimestampOverride = Some(timeProvider.currentTimestamp + 100000)
          engine.executeTasksTask()
          Get(s"/tasks?parent_entity_id=${pe.entity_id}") ~> engine.webRoutes.route ~> check {
            val tasks = r[List[Task]]
            tasks.size shouldEqual 1
            tasks.head.finished shouldBe true
            Post(s"/tasks/${tasks.head.task_id}/ack") ~> engine.webRoutes.route ~> check {
              val task = r[Task]
              task.acknowledged shouldBe true
            }
          }
        }
      })
    }

    "allow creation of a task with data against an entity" in {
      createPlayer((player, pe) => {
        Put(s"/tasks?parent_entity_id=${pe.entity_id}", CreateTaskRequest(1, Some("{\"test\":2}".parseJson))) ~> engine.webRoutes.route ~> check {
          handled shouldBe true
          Get(s"/tasks?parent_entity_id=${pe.entity_id}") ~> engine.webRoutes.route ~> check {
            val tasks = r[List[Task]]
            tasks.exists(_.data == "{\"test\":2}".parseJson) shouldBe true
          }
        }
      })
    }

    "generate requirements for an aggregateable entity when amount is used" in {
      createPlayer((player, pe) => {
        Put(s"/entities", CreateEntityRequest("planets", Some(pe.entity_id), Some(player.entity_id))) ~> engine.webRoutes.route ~> check {
          Get(s"/entities?primaryParentEntityId=${player.entity_id}") ~> engine.webRoutes.route ~> check {
            val entities = r[List[Entity]]
            val planetEntity = entities.find(_.id == "planets").get
            Get(s"/entities?parent_entity_id=${planetEntity.entity_id}&playerId=${player.entity_id}") ~> engine.webRoutes.route ~> check {
              val crystalMineCreationResponse = r[List[Entity]].filter(_.id == "crystal-mine").head
              Get(s"/entities/${crystalMineCreationResponse.entity_id}/requirements") ~> engine.webRoutes.route ~> check {
                val requirements = r[RequirementsResponse]
                requirements.attributes.size shouldEqual 1
                requirements.attributes.find(_.requirement.id == "parent[crystal]").flatMap(_.requirement.formula) shouldBe Some("100.0")
              }
            }
          }
        }
      })
    }

    "generate requirements for an aggregateable entity" in {
      createPlayer((player, pe) => {
        Put(s"/entities", CreateEntityRequest("planets", Some(pe.entity_id), Some(player.entity_id))) ~> engine.webRoutes.route ~> check {
          Get(s"/entities?primaryParentEntityId=${player.entity_id}") ~> engine.webRoutes.route ~> check {
            val entities = r[List[Entity]]
            val planetEntity = entities.find(_.id == "planets").get
            Get(s"/entities?parent_entity_id=${planetEntity.entity_id}&playerId=${player.entity_id}") ~> engine.webRoutes.route ~> check {
              val metalMineCreationResponse = r[List[Entity]].filter(_.id == "metal-mine").head
              Get(s"/entities/${metalMineCreationResponse.entity_id}/requirements") ~> engine.webRoutes.route ~> check {
                val requirements = r[RequirementsResponse]
                requirements.attributes.size shouldEqual 2
                requirements.attributes.find(_.requirement.id == "parent[crystal]").flatMap(_.requirement.formula) shouldBe Some("99.0")
                requirements.attributes.find(_.requirement.id == "parent.parent[dark_matter]").flatMap(_.requirement.formula) shouldBe Some("5.0")
                requirements.attributes.forall(_.fulfilled) shouldBe true
                requirements.entities.forall(_.fulfilled) shouldBe true
                requirements.fulfilled shouldBe true
              }
            }
          }
        }
      })
    }

    "upgrade an entity" in {
      createPlayer((player, pe) => {
        Put(s"/entities", CreateEntityRequest("planets", Some(pe.entity_id), Some(player.entity_id))) ~> engine.webRoutes.route ~> check {
          Get(s"/entities?primaryParentEntityId=${player.entity_id}") ~> engine.webRoutes.route ~> check {
            val entities = r[List[Entity]]
            val planetEntity = entities.find(_.id == "planets").get
            val crystal = planetEntity.attributes.find(_.attr == "crystal").get.value.get.toInt
            crystal shouldBe 100
            Get(s"/entities?parent_entity_id=${planetEntity.entity_id}") ~> engine.webRoutes.route ~> check {
              val metalMine = r[List[Entity]].filter(_.id == "metal-mine").head
              metalMine.amount shouldBe 0
              Post(s"/entities/${metalMine.entity_id}/requirements") ~> engine.webRoutes.route ~> check {
                Post(s"/entities/${metalMine.entity_id}/upgrade/2") ~> engine.webRoutes.route ~> check {
                  Post(s"/entities", EntitiesQuery(parent_entity_id = Some(planetEntity.entity_id))) ~> engine.webRoutes.route ~> check {
                    val metalMine2 = r[List[Entity]].filter(_.id == "metal-mine").head
                    metalMine2.amount shouldBe 1
                    Get(s"/entities/${planetEntity.entity_id}") ~> engine.webRoutes.route ~> check {
                      val crystal = r[Entity].attributes.find(_.attr == "crystal").get.value.get.toDouble
                      crystal.toInt shouldBe 1
                    }
                  }
                }
              }
            }
          }
        }
      })
    }

    "update crystal-hourly" in {
      createPlayer((player, pe) => {
        Put(s"/entities", CreateEntityRequest("planets", Some(pe.entity_id), Some(player.entity_id))) ~> engine.webRoutes.route ~> check {
          val planet = r[EntityCreationResponse]
          Post(s"/entities/${planet.entity_id}/attributes/crystal-hourly/500") ~> engine.webRoutes.route ~> check {
            timeProvider.currentTimestampOverride = Some(timeProvider.currentTimestamp + 100000)
            engine.executeHourlyRateUpdate()
            Get(s"/entities/${planet.entity_id}") ~> engine.webRoutes.route ~> check {
              val entitiesAfter = r[Entity]
              entitiesAfter.attributes.find(_.attr == "crystal-hourly").get.value.get.toDouble shouldBe 500
              entitiesAfter.attributes.find(_.attr == "crystal").get.value.get.toDouble > 100 shouldBe true
            }
          }
        }
      })
    }
  }
}