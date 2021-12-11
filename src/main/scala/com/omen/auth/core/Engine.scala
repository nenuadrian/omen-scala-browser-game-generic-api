package com.omen.auth.core

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContextExecutor

class Engine(ds: BasicDataSource) extends Logging {
  private val playerConnPool = ds
  private val sessionConnPool = ds

  logger.info(
    """
      | _______  __   __  _______  __    _
      ||       ||  |_|  ||       ||  |  | |
      ||   _   ||       ||    ___||   |_| |
      ||  | |  ||       ||   |___ |       |
      ||  |_|  ||       ||    ___||  _    |
      ||       || ||_|| ||   |___ | | |   |
      ||_______||_|   |_||_______||_|  |__|""".stripMargin)


  implicit val system: ActorSystem = ActorSystem("omen-as")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private val authService = new AuthService(Map("player" -> playerConnPool), sessionConnPool)

  val webRoutes = new AuthRoutes(authService)
}