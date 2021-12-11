package com.omen.auth.core

import java.sql.Timestamp

class PasswordExpiryPolicyBreachException extends Exception

class AccessDeniedException extends Exception

class SessionExpiredException extends Exception

class EmailInUseException extends Exception

class RequestLimitReachedException extends Exception

class AccountLockedException extends Exception

case class LoginRequest(email: String, password: String)

case class LoginResult(status: String = "success", session: Option[String] = None)

case class ChangePasswordRequest(old_password: String, new_password: String)

case class SessionValidationResponse(status: String = "success", account: User, session: String)

case class SignupResponse(account_id: Int, session: String)

case class Session(session: String, account_id: Int, account_type: String, last_access: Timestamp)

case class SignupRequest(email: String, password: String)

case class ResetPasswordRequest(password: String)

case class ForgotPasswordResult(hash: String)

case class ConfirmEmailResult(account_id: Int)

case class ResetEmailConfirmationResult(hash: String)

case class User(account_id: Int, group_id: Int, status: Int, email: String,
                email_confirmed: Boolean = false,
                till_password_policy_breach: Long = Long.MaxValue, password: String, var session: Option[Session] = None)

class Player(override val account_id: Int, override val group_id: Int, override val status: Int, override val email: String,
             override val email_confirmed: Boolean, override val password: String)
  extends User(account_id, group_id, status, email, email_confirmed, password = password)

