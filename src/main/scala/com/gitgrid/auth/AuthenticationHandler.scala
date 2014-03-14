package com.gitgrid.auth

import com.gitgrid.Config
import com.gitgrid.models._
import com.gitgrid.utils.NonceGenerator
import reactivemongo.bson.BSONObjectID
import scala.concurrent.{ExecutionContext, Future}

class AuthenticationHandler(implicit config: Config, ec: ExecutionContext) {
  val db = Database()

  def authenticate(userName: String, password: String): Future[Option[User]] = {
    db.users.findByUserName(userName).flatMap {
      case Some(u) =>
        db.userPasswords.findCurrentPassword(u.id.get).map {
          case Some(pwd) => if (checkPassword(pwd, password)) Some(u) else None
          case _ => None
        }
      case _ =>
        Future.successful(None)
    }
  }

  def checkPassword(userPassword: UserPassword, password: String): Boolean = userPassword.hashAlgorithm match {
    case "plain" if userPassword.hash == password => true
    case _ => false
  }

  def findUser(id: BSONObjectID): Future[Option[User]] = db.users.find(id)
  def findUser(userName: String): Future[Option[User]] = db.users.findByUserName(userName)

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
