package com.gitgrid.auth

import com.gitgrid.Config
import com.gitgrid.models._
import com.gitgrid.utils.NonceGenerator
import reactivemongo.bson.{BSONDateTime, BSONObjectID}
import scala.concurrent.{ExecutionContext, Future}

class AuthenticationHandler(db: Database)(implicit ec: ExecutionContext) {
  def authenticate(userName: String, password: String): Future[Option[User]] = {
    db.users.findByUserName(userName).flatMap {
      case Some(user) =>
        checkPassword(user, password).map {
          case true => Some(user)
          case false => None
        }
      case _ =>
        Future.successful(None)
    }
  }

  def setPassword(user: User, newPassword: String): Future[Unit] = {
    val now = BSONDateTime(System.currentTimeMillis)
    db.userPasswords.insert(UserPassword(userId = user.id.get, createdAt = now, hash = newPassword, hashAlgorithm = "plain")).map(_ => Unit)
  }

  def checkPassword(user: User, password: String): Future[Boolean] = {
    db.userPasswords.findCurrentPassword(user.id.get).map {
      case Some(pwd) =>
        pwd.hashAlgorithm match {
          case "plain" if pwd.hash == password => true
          case _ => false
        }
      case _ => throw new Exception("Could not find current user password")
    }
  }

  def createSession(userId: BSONObjectID): Future[Session] = {
    val sessionId = NonceGenerator.generateString(16)
    db.sessions.insert(new Session(userId = userId, sessionId = sessionId, expires = None))
  }

  def revokeSession(sessionId: String): Future[Unit] = {
    db.sessions.deleteBySessionId(sessionId)
  }

  def findSession(sessionId: String): Future[Option[Session]] = {
    db.sessions.findBySessionId(sessionId)
  }
}

object AuthenticationHandler {
  def apply()(implicit config: Config, ec: ExecutionContext): AuthenticationHandler = new AuthenticationHandler(Database())
}
