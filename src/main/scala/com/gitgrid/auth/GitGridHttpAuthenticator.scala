package com.gitgrid.auth

import com.gitgrid.models._
import scala.concurrent._
import spray.routing.AuthenticationFailedRejection._
import spray.routing._
import spray.routing.authentication._

class GitGridHttpAuthenticator(db: Database)(implicit ec: ExecutionContext) extends ContextAuthenticator[User] {
  val realm = "gitgrid"
  val cookieName = "gitgrid-sid"
  val cookiePath = "/"

  val userPassAuthenticator = new GitGridUserPassAuthenticator(db)
  val basicAuthenticator = new BasicHttpAuthenticator[User](realm, userPassAuthenticator)
  val sessionHandler = new SessionHandler(db, cookieName, cookiePath)

  def apply(ctx: RequestContext): Future[Authentication[User]] = {
    authenticateByBasicHttp(ctx).flatMap {
      case Right(user) => acceptFuture(user)
      case _ => authenticateBySession(ctx)
    }
  }

  def authenticateByBasicHttp(ctx: RequestContext): Future[Authentication[User]] = {
    basicAuthenticator(ctx)
  }

  def authenticateBySession(ctx: RequestContext): Future[Authentication[User]] = {
    sessionHandler.extractSessionId(ctx.request) match {
      case Some(sessionId) =>
        sessionHandler.findSession(sessionId)
          .flatMap[Authentication[User]] {
            case Some(session) => db.users.find(session.userId).map {
              case Some(user) => accept(user)
              case _ => reject(CredentialsRejected)
            }
            case _ => rejectFuture(CredentialsRejected)
          }
      case _ => rejectFuture(CredentialsMissing)
    }
  }

  private def accept(user: User): Authentication[User] = Right[Rejection, User](user)
  private def reject(cause: Cause): Authentication[User] = Left[Rejection, User](AuthenticationFailedRejection(cause, Nil))
  private def acceptFuture(user: User): Future[Authentication[User]] = Future.successful(accept(user))
  private def rejectFuture(cause: Cause): Future[Authentication[User]] = Future.successful(reject(cause))
}

