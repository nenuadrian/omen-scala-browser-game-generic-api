package core.api

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{RawHeader, `Access-Control-Allow-Credentials`, `Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`}
import akka.http.scaladsl.server.{Directives, Route}

trait CORSSupport extends Directives {
  private val CORSHeaders = List(
    `Access-Control-Allow-Methods`(GET, POST, PUT, DELETE, OPTIONS),
    `Access-Control-Allow-Headers`("Origin, X-Requested-With, Content-Type, Accept, Accept-Encoding, Accept-Language, Host, Referer, User-Agent"),
    `Access-Control-Allow-Credentials`(true),
    RawHeader("Access-Control-Allow-Origin", "*")
  )

  def respondWithCORS(routes: => Route) = {
    respondWithHeaders(CORSHeaders) {
      routes ~ options {
        complete(StatusCodes.OK)
      }
    }
  }
}
