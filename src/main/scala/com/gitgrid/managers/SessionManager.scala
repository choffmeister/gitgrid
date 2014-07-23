package com.gitgrid.managers

import com.gitgrid.models._
import com.gitgrid.utils.NonceGenerator
import reactivemongo.bson.BSONObjectID
import scala.concurrent.{ExecutionContext, Future}
import spray.http.HttpRequest

class SessionManager(db: Database)(implicit ec: ExecutionContext) {
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

  def findUser(sessionId: String): Future[Option[User]] = {
    db.sessions.findBySessionId(sessionId).flatMap {
      case Some(session) => db.users.find(session.userId)
      case None => Future(None)
    }
  }
}
