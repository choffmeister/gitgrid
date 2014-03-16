package com.gitgrid.auth

import com.gitgrid.models.{Database, Session}
import com.gitgrid.utils.NonceGenerator
import reactivemongo.bson.BSONObjectID
import scala.concurrent.{ExecutionContext, Future}
import spray.http.HttpRequest

class SessionHandler(db: Database, val cookieName: String, val cookiePath: String = "/")(implicit ec: ExecutionContext) {
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

  def extractSessionId(request: HttpRequest): Option[String] = {
    request.cookies.find(c => c.name == cookieName).map(_.content)
  }
}
