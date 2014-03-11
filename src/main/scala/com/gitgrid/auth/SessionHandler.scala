package com.gitgrid.auth

import com.gitgrid.Config
import com.gitgrid.models.{Database, Session}
import java.security.SecureRandom
import reactivemongo.bson._
import scala.concurrent.{ExecutionContext, Future}

class SessionHandler(implicit config: Config, ec: ExecutionContext) {
  val db = Database()

  def createSession(userId: BSONObjectID): Future[Session] = {
    val sessionId = SessionHandler.generateSessionId()
    db.sessions.insert(new Session(id = Some(BSONObjectID.generate), userId = userId, sessionId = sessionId, expires = None))
  }

  def revokeSession(sessionId: String): Future[Unit] = {
    db.sessions.deleteBySessionId(sessionId)
  }

  def findSession(sessionId: String): Future[Option[Session]] = {
    db.sessions.findBySessionId(sessionId)
  }
}

object SessionHandler {
  lazy val random = new SecureRandom()

  def generateSessionId(): String = {
    val bin = new Array[Byte](16)
    random.nextBytes(bin)
    bin.map("%02X" format _).mkString
  }
}
