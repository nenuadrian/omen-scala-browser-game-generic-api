package com.omen.auth.core

import com.github.t3hnar.bcrypt._
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.logging.log4j.scala.Logging

import java.sql.{ResultSet, Statement, Timestamp}
import java.time.Instant
import java.util.Date
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import scala.util.Try

abstract class UserService(val connPool: BasicDataSource, sessionConnPool: BasicDataSource) {
  def sessionExpiry: Duration = 1 hour

  def mustConfirmEmail: Boolean

  def passwordExpiryDaysPolicy: Int

  def accountTable: String

  def accountType: String

  def accountKey: String

  def resendEmailConfirmation(account_id: Int): String = {
    sendEmailConfirmation(account_id)
  }

  private def sendEmailConfirmation(account_id: Int): String = {
    val sessionConn = sessionConnPool.getConnection
    try {
      /* val checkStatement = sessionConn.prepareStatement(
         s"""SELECT COUNT(*) AS checkCount FROM email_confirmation WHERE account_type = ?
            |AND account_id = ?""".stripMargin)
       checkStatement.setString(1, accountType)
       checkStatement.setInt(2, account_id)
       val checkRs = checkStatement.executeQuery
       checkRs.next()

       if (checkRs.getInt("checkCount") >= 3) {
         throw new RequestLimitReachedException
       }*/

      val token = randomUUID().toString
      val statement2 = sessionConn.prepareStatement("INSERT INTO email_confirmation (account_type, account_id, token) VALUES (?, ?, ?)")
      statement2.setString(1, accountType)
      statement2.setInt(2, account_id)
      statement2.setString(3, token)
      statement2.execute()
      token
    } finally {
      sessionConn.close()
    }
  }

  def confirmEmail(token: String): Int = {
    val conn = connPool.getConnection
    val sessionConn = sessionConnPool.getConnection
    try {
      val statement = sessionConn.prepareStatement(s"SELECT * FROM `email_confirmation` WHERE token = ? AND disabled_at IS NULL AND account_type = '$accountType'")
      statement.setString(1, token)
      val rs = statement.executeQuery
      rs.next()
      val account_id = rs.getInt("account_id")

      val statement3 = conn.prepareStatement(s"UPDATE $accountTable SET email_confirmed_at = NOW() WHERE $accountKey = ? LIMIT 1")
      statement3.setInt(1, account_id)
      statement3.execute()

      val statement2 = sessionConn.prepareStatement(s"UPDATE email_confirmation SET disabled_at = NOW() WHERE id = ? LIMIT 1")
      statement2.setInt(1, rs.getInt("id"))
      statement2.execute()

      account_id
    } finally {
      conn.close(); sessionConn.close()
    }
  }

  def resetPassword(token: String, newPass: String): Unit = {
    val sessionConn = sessionConnPool.getConnection
    try {
      val statement = sessionConn.prepareStatement(s"SELECT * FROM `forgot_password` WHERE token = ? AND disabled_at IS NULL AND account_type = '$accountType'")
      statement.setString(1, token)
      val rs = statement.executeQuery
      rs.next()

      changePassword(rs.getInt("account_id"), newPass)

      val statement2 = sessionConn.prepareStatement(s"UPDATE forgot_password SET disabled_at = NOW() WHERE id = ? LIMIT 1")
      statement2.setInt(1, rs.getInt("id"))
      statement2.execute()
    } finally {
      sessionConn.close()
    }
  }

  def forgotPassword(email: String): String = {
    val conn = connPool.getConnection
    val sessionConn = sessionConnPool.getConnection
    try {
      val statement = conn.prepareStatement(s"SELECT * FROM `$accountTable` WHERE email = ?")
      statement.setString(1, email)
      val rs = statement.executeQuery

      if (!rs.next()) throw new AccessDeniedException

      /*val checkStatement = sessionConn.prepareStatement(
        s"""SELECT COUNT(*) AS checkCount FROM forgot_password WHERE account_type = ?
           |AND account_id = ? AND created_at >= TIMESTAMP(DATE_SUB(NOW(), INTERVAL 2 hour))""".stripMargin)
      checkStatement.setString(1, accountType)
      checkStatement.setInt(2, rs.getInt(accountKey))
      val checkRs = checkStatement.executeQuery
      checkRs.next()

      if (checkRs.getInt("checkCount") >= 3) {
        throw new RequestLimitReachedException
      }*/

      val token = randomUUID().toString
      val statement2 = sessionConn.prepareStatement("INSERT INTO forgot_password (account_type, account_id, token) VALUES (?, ?, ?)")
      statement2.setString(1, accountType)
      statement2.setInt(2, rs.getInt(accountKey))
      statement2.setString(3, token)
      statement2.execute()
      token
    } finally {
      conn.close(); sessionConn.close()
    }
  }

  def signup(request: SignupRequest): Session = {
    val conn = sessionConnPool.getConnection
    try {
      findAccountByEmail(request.email) match {
        case None => {
          val statement = conn.prepareStatement(s"INSERT INTO $accountTable (email, password) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)
          statement.setString(1, request.email)
          statement.setString(2, request.password.bcrypt)
          statement.execute()
          val generatedKeys = statement.getGeneratedKeys
          generatedKeys.next()

          val account_id = generatedKeys.getInt(1)

          createSession(account_id)
        }
        case Some(_) => throw new EmailInUseException
      }
    } finally {
      conn.close()
    }
  }

  def changePassword(account_id: Int, pass: String) = {
    val conn = connPool.getConnection
    try {
      val statement = conn.prepareStatement(s"UPDATE $accountTable SET password = ?, last_password_change = NOW() WHERE $accountKey = ? LIMIT 1")
      statement.setString(1, pass.bcrypt)
      statement.setInt(2, account_id)
      statement.execute()
    } finally {
      conn.close()
    }
  }

  private def findAccountByEmail(email: String): Option[User] = {
    val conn = connPool.getConnection
    try {
      val statement = conn.prepareStatement(s"SELECT * FROM `$accountTable` WHERE email = ?")
      statement.setString(1, email)
      val rs = statement.executeQuery
      if (!rs.next()) throw new AccessDeniedException
      Some(rsToUser(rs))
    } catch {
      case _: Throwable => None
    } finally {
      conn.close()
    }
  }

  def login(email: String, password: String, ip: String): User = {
    val sessionConn = sessionConnPool.getConnection
    try {
      /*val statement = sessionConn.prepareStatement(s"""SELECT COUNT(*) AS checkCount FROM log WHERE identifier = 'login-failed'
         |AND data = ? AND created_at >= DATE_SUB(NOW(), INTERVAL 30 minute)""".stripMargin)
      statement.setString(1, ip)
      val checkRs = statement.executeQuery
      checkRs.next()

      if (checkRs.getInt("checkCount") >= 10) {
        throw new RequestLimitReachedException
      }*/

      findAccountByEmail(email) match {
        case Some(user) => {
          if (password.isBcrypted(user.password)) {
            val session = createSession(user.account_id)
            user.copy(session = Some(session))
          } else throw new AccessDeniedException
        }
        case None => throw new AccessDeniedException
      }
    } catch {
      case e: AccessDeniedException => {
        val statement = sessionConn.prepareStatement(s"INSERT INTO log (identifier, data) VALUES ('login-failed', ?)")
        statement.setString(1, ip)
        statement.execute()
        throw e
      }
    } finally {
      sessionConn.close()
    }
  }

  def rsToUser(rs: ResultSet): User

  def fetchUser(session: Session): User = {
    val conn = connPool.getConnection
    try {
      val statement2 = conn.prepareStatement(s"SELECT * FROM `$accountTable` WHERE $accountKey = ?")
      statement2.setInt(1, session.account_id)
      val rs2 = statement2.executeQuery
      rs2.next()
      rsToUser(rs2)
    } finally {
      conn.close()
    }
  }

  def computePasswordPolicy(last_password_change: Date): Long = {
    val now = new Timestamp(new Date().getTime)
    val diff = now.getTime - last_password_change.getTime
    passwordExpiryDaysPolicy * 24 - TimeUnit.HOURS.convert(diff, TimeUnit.MILLISECONDS)
  }

  def createSession(accountId: Int): Session = {
    val conn = sessionConnPool.getConnection
    try {
      val id = randomUUID().toString
      val statement = conn.prepareStatement("INSERT INTO session (account_type, account_id, session_id) VALUES (?, ?, ?)")
      statement.setString(1, accountType)
      statement.setString(2, accountId.toString)
      statement.setString(3, id)
      statement.execute()
      Session(id, accountId, accountType, new Timestamp(new java.util.Date().getTime))
    } finally {
      conn.close()
    }
  }
}

class PlayerService(connPool: BasicDataSource, sessionConnPool: BasicDataSource) extends UserService(connPool, sessionConnPool) {
  def passwordExpiryDaysPolicy: Int = Int.MaxValue

  def accountTable = "user"

  def accountType = "player"

  def accountKey = "user_id"

  def mustConfirmEmail = true

  def rsToUser(rs: ResultSet): Player = {
    new Player(
      account_id = rs.getInt(accountKey),
      group_id = rs.getInt("group_id"),
      status = rs.getInt("status"),
      email = rs.getString("email"),
      email_confirmed = rs.getDate("email_confirmed_at") match {
        case null => false
        case _ => true
      },
      password = rs.getString("password")
    )
  }
}

class SessionService(sessionConnPool: BasicDataSource) {
  val sessionCache: TrieMap[String, Session] = scala.collection.concurrent.TrieMap[String, Session]()

  def getSession(session_hash: String): Session = {
    if (sessionCache.contains(session_hash)) {
      sessionCache(session_hash)
    } else {
      val conn = sessionConnPool.getConnection
      try {
        val statement = conn.prepareStatement("SELECT * FROM `session` WHERE session_id = ? AND status = 1")
        statement.setString(1, session_hash)
        val rs = statement.executeQuery
        if (!rs.next()) throw new AccessDeniedException

        Session(
          session_hash,
          rs.getString("account_id").toInt,
          rs.getString("account_type"),
          last_access = Timestamp.from(Instant.now())
        )
      } finally {
        conn.close()
      }
    }
  }

  def validateSession(session: Session, expiry: Duration) = {
    if (Timestamp.from(Instant.now()).getTime - session.last_access.getTime >= expiry.toMillis) {
      val conn = sessionConnPool.getConnection
      try {
        val statement = conn.prepareStatement("UPDATE `session` SET status = 3 WHERE session_id = ?")
        statement.setString(1, session.session)
        statement.executeUpdate()
      } finally {
        conn.close()
      }

      throw new SessionExpiredException
    }

    if (sessionCache.contains(session.session)) {
      sessionCache.remove(session.session)
    }
    sessionCache.put(session.session, session.copy(last_access = Timestamp.from(Instant.now())))
  }

  def logout(session: Session): Unit = {
    val conn = sessionConnPool.getConnection
    try {
      val statement = conn.prepareStatement("UPDATE `session` SET status = 2 WHERE session_id = ?")
      statement.setString(1, session.session)
      statement.executeUpdate()
    } finally {
      conn.close()
    }

    sessionCache.remove(session.session)
  }
}

class AuthService(dbPools: Map[String, BasicDataSource], sessionConnPool: BasicDataSource) extends Logging {

  val services: Map[String, PlayerService] = List("player")
    .map(e => e -> new PlayerService(dbPools.getOrElse(e, throw new Exception(s"DB Pool not found for $e")), sessionConnPool)).toMap
  val sessionService = new SessionService(sessionConnPool)

  def login(email: String, password: String, accountType: String, ip: String): Try[User] = Try {
    val user = services(accountType).login(email, password, ip)
    if (user.status == 2) throw new AccountLockedException
    if (user.till_password_policy_breach < 0) throw new PasswordExpiryPolicyBreachException
    user
  }

  def validate(session_hash: String): Try[User] = Try {
    val session = sessionService.getSession(session_hash)
    sessionService.validateSession(session, services(session.account_type).sessionExpiry)

    val user = services(session.account_type).fetchUser(session)

    if (user.status == 2) throw new AccessDeniedException
    if (user.till_password_policy_breach < 0) throw new PasswordExpiryPolicyBreachException

    user.session = Some(session)
    user
  }

  def changePassword(user: User, request: ChangePasswordRequest): Unit = {
    if (request.old_password.isBcrypted(user.password)) {
      services(user.session.get.account_type).changePassword(user.account_id, request.new_password)
    } else throw new AccessDeniedException
  }

  def signup(request: SignupRequest, accountType: String): Session = {
    services(accountType).signup(request)
  }

  def forgotPass(accountType: String, email: String): String = {
    services(accountType).forgotPassword(email)
  }

  def confirmEmail(accountType: String, hash: String): Int = {
    services(accountType).confirmEmail(hash)
  }

  def resetPass(accountType: String, token: String, newPassword: String): Unit = {
    services(accountType).resetPassword(token, newPassword)
  }

  def resendEmailConfirmation(account_type: String, account_id: Int): String = {
    services(account_type).resendEmailConfirmation(account_id)
  }

  def logout(user: User): Unit = {
    sessionService.logout(user.session.get)
  }
}