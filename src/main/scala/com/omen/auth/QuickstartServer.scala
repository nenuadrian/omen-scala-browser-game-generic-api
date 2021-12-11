package com.omen.auth

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.omen.auth.core.{Engine, H2Database}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContextExecutor
import scala.util.Properties

class QuickstartServer

import org.apache.commons.dbcp2._

object QuickstartServer extends App with Logging with H2Database {
  val port = Properties.envOrElse("PORT", 8094.toString).toInt

  val ds = generateDataSource
  refresh(ds)

  logger.info(s"OMEN Auth service online on $port")

  implicit val system: ActorSystem = ActorSystem("omen-as")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val engine = new Engine(ds)
  private val bindingFuture = Http().bindAndHandle(engine.webRoutes.route, "localhost", port)

  private def mysql: BasicDataSource = {
    val clientConnPool = new BasicDataSource()
    val jdbcSettings = "?&autoReconnect=true&failOverReadOnly=false&maxReconnects=10"
    val urlClient = "jdbc:mysql://localhost" + jdbcSettings
    val driver = "com.mysql.jdbc.Driver"
    clientConnPool.setDriverClassName(driver)
    clientConnPool.setUrl(urlClient)
    clientConnPool.setInitialSize(2)
    clientConnPool.setUsername("root")
    clientConnPool.setPassword("root")
    clientConnPool
  }
}
