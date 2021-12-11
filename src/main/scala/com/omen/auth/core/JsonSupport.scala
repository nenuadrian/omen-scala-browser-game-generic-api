package com.omen.auth.core

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsBoolean, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

object JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val LoginRequestFormat = jsonFormat2(LoginRequest)
  implicit val LoginResultFormat = jsonFormat2(LoginResult)
  implicit val ChangePasswordRequestFormat = jsonFormat2(ChangePasswordRequest)
  implicit val ResetPasswordRequestFormat = jsonFormat1(ResetPasswordRequest)
  implicit val SignupRequestFormat = jsonFormat2(SignupRequest)
  implicit val SignupResponseFormat = jsonFormat2(SignupResponse)
  implicit val ForgotPasswordResultFormat = jsonFormat1(ForgotPasswordResult)
  implicit val ConfirmEmailResultFormat = jsonFormat1(ConfirmEmailResult)
  implicit val ResetEmailConfirmationResultFormat = jsonFormat1(ResetEmailConfirmationResult)

  implicit object UserFormat extends RootJsonFormat[User] {
    def write(user: User) = user match {
      case client: Player => JsObject(
        "user_id" -> JsNumber(client.account_id),
        "email" -> JsString(client.email),
        "email_confirmed" -> JsBoolean(client.email_confirmed),
        "group_id" -> JsNumber(client.group_id))
      case _ => throw new UnsupportedOperationException
    }

    def read(value: JsValue) = null
  }

  implicit val SessionValidationResponseFormat = jsonFormat3(SessionValidationResponse)
}
