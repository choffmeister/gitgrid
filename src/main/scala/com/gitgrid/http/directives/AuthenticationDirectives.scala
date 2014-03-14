package com.gitgrid.http.directives

import com.gitgrid.auth._
import com.gitgrid.models._
import reactivemongo.bson._
import scala.concurrent._
import spray.http._
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import spray.routing.Directives._
import spray.routing._

trait AuthenticationDirectives {
  implicit val executor: ExecutionContext
  val db: Database
  val auth: AuthenticationHandler

  val cookiePath = "/"
  val cookieName = "gitgrid-sid"

  def createSessionCookie(session: Session): Directive0 = {
    def convertDateTime(dt: Option[BSONDateTime]) = dt match {
      case Some(dt) => Some(DateTime(dt.value))
      case _ => None
    }

    setCookie(HttpCookie(cookieName, session.sessionId, expires = convertDateTime(session.expires), path = Some(cookiePath)))
  }

  def removeSessionCookie(): Directive0 = {
    deleteCookie(cookieName, path = cookiePath)
  }

  def extractSessionId: Directive1[Option[String]] = {
    extract(_.request.cookies.find(c => c.name == cookieName)).map {
      case Some(cookie) => Some(cookie.content)
      case _ => None
    }
  }

  def authenticateUserOption: Directive1[Option[User]] = {
    extractSessionId.flatMap {
      case Some(sessionId) =>
        val future = auth
          .findSession(sessionId)
          .flatMap[Option[User]] {
            case Some(session) => db.users.find(session.userId)
            case _ => Future.successful(None)
          }

        onSuccess(future)
      case _ => provide(None)
    }
  }

  def authenticateUser: Directive1[User] =
    authenticateUserOption.flatMap {
      case Some(u) => provide(u)
      case _ => reject(AuthenticationFailedRejection(CredentialsRejected, Nil))
    }
}
