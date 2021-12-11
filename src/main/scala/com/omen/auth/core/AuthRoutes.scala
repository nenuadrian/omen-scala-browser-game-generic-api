package com.omen.auth.core

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.omen.auth.core.JsonSupport._
import org.apache.logging.log4j.scala.Logging
import spray.json.{JsNumber, JsObject}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class AuthRoutes(authService: AuthService)(implicit system: ActorSystem, materializer: ActorMaterializer,
                                           executionContext: ExecutionContextExecutor) extends SprayJsonSupport with Logging {

  implicit lazy val timeout: Timeout = Timeout(5.seconds)

  def route: Route = {
    def myUserAuthenticator(credentials: Credentials): Option[User] =
      credentials match {
        case Credentials.Provided(session) => {
          authService.validate(session) match {
            case Success(u) => Some(u)
            case Failure(_) => None
          }
        }
        case Credentials.Missing => None
      }

    concat(
      path("ping") {
        get {
          complete(JsObject("status" -> JsNumber(200)))
        }
      },
      path("login" / Segment / Segment) { (accountType, ip) =>
        post {
          entity(as[LoginRequest]) { loginRequest =>
            complete {
              logger.info(s"Auth request received")
              authService.login(loginRequest.email, loginRequest.password, accountType, ip) match {
                case Success(u) => LoginResult(session = Some(u.session.get.session))
                case Failure(e) => e match {
                  case _: PasswordExpiryPolicyBreachException => LoginResult(status = "PasswordExpiryPolicyBreachException")
                  case _: AccountLockedException => LoginResult(status = "AccountLockedException")
                  case _: RequestLimitReachedException => LoginResult(status = "RequestLimitReachedException")
                  case _: AccessDeniedException => LoginResult(status = "AccessDeniedException")
                  case e: Throwable => {
                    logger.error(e)
                    LoginResult(status = "UnknownException")
                  }
                }
              }
            }
          }
        }
      },
      path("signup" / Segment) { accountType =>
        post {
          entity(as[SignupRequest]) { sRequest =>
            complete {
              logger.info(s"Signup request received")
              try {
                val session = authService.signup(sRequest, accountType)
                SignupResponse(account_id = session.account_id, session = session.session)
              } catch {
                case _: EmailInUseException => LoginResult(status = "EmailInUseException")
                case e: Throwable => LoginResult(status = "UnknownException" + e.getMessage)
              }
            }
          }
        }
      },
      path("forgot-password" / Segment / Segment) { (accountType, email) => {
        post {
          complete {
            logger.info(s"Forgot Pass request received")
            try {
              val hash = authService.forgotPass(accountType, email)
              ForgotPasswordResult(hash = hash)
            } catch {
              case e: Throwable => LoginResult(status = "AccessDeniedException")
            }
          }
        }
      }
      },
      path("reset" / Segment / Segment) { (accountType, token) => {
        post {
          entity(as[ResetPasswordRequest]) { rpRequest =>
            complete {
              logger.info(s"Reset Pass request received")
              try {
                authService.resetPass(accountType, token, rpRequest.password)
                LoginResult()
              } catch {
                case _: Throwable => LoginResult(status = "AccessDeniedException")
              }
            }
          }
        }
      }
      },
      path("confirm" / Segment / Segment) { (accountType, token) => {
        post {
          complete {
            logger.info(s"Confirm request received")
            try {
              ConfirmEmailResult(authService.confirmEmail(accountType, token))
            } catch {
              case _: Throwable => LoginResult(status = "AccessDeniedException")
            }
          }
        }
      }
      },
      Route.seal {
        authenticateBasic(realm = "secure site", myUserAuthenticator) { user =>
          concat(
            path("resend-email-confirmation") {
              get {
                complete {
                  logger.info(s"Resend email conf request received")
                  try {
                    ResetEmailConfirmationResult(hash = authService.resendEmailConfirmation(user.session.get.account_type, user.account_id))
                  } catch {
                    case _: Throwable => LoginResult(status = "RequestLimitReachedException")
                    case _: Throwable => LoginResult(status = "AccessDeniedException")
                  }
                }
              }
            },
            path("validate") {
              get {
                complete {
                  logger.info(s"Session validation request received")
                  logger.info(s"Account type: ${user.session.get.account_type}")
                  try {
                    SessionValidationResponse(account = user, session = user.session.get.session)
                  } catch {
                    case _: Throwable => LoginResult(status = "AccessDeniedException")
                  }
                }
              }
            },
            path("logout") {
              get {
                complete {
                  logger.info(s"Logout")
                  try {
                    authService.logout(user)
                    LoginResult()
                  } catch {
                    case _: Throwable => LoginResult(status = "AccessDeniedException")
                  }
                }
              }
            },
            path("change-password") {
              post {
                entity(as[ChangePasswordRequest]) { cpRequest =>
                  complete {
                    logger.info(s"Change password request received")

                    try {
                      authService.changePassword(user, cpRequest)
                      LoginResult()
                    } catch {
                      case _: Throwable => LoginResult(status = "AccessDeniedException")
                    }
                  }
                }
              }
            })
        }
      })
  }
}
