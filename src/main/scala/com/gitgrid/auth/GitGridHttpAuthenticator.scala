package com.gitgrid.auth

import com.gitgrid.managers._
import com.gitgrid.models._
import scala.concurrent._
import spray.routing.AuthenticationFailedRejection._
import spray.routing._
import spray.routing.authentication._

class GitGridHttpAuthenticator(db: Database)(implicit ec: ExecutionContext) extends ContextAuthenticator[User] {
  val realm = "gitgrid"
  val cookieName = "gitgrid-sid"
  val cookiePath = "/"

  val sessionManager = new SessionManager(db)
  val userManager = new UserManager(db)
  val userPassAuthenticator = new GitGridUserPassAuthenticator(userManager)
  val basicAuthenticator = new BasicHttpAuthenticator[User](realm, userPassAuthenticator)

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
    ctx.request.cookies.find(c => c.name == cookieName).map(_.content) match {
      case Some(sessionId) =>
        sessionManager.findUser(sessionId).map {
          case Some(user) => accept(user)
          case _ => reject(CredentialsRejected)
        }
      case _ => rejectFuture(CredentialsMissing)
    }
  }

  private def accept(user: User): Authentication[User] = Right[Rejection, User](user)
  private def reject(cause: Cause): Authentication[User] = Left[Rejection, User](AuthenticationFailedRejection(cause, Nil))
  private def acceptFuture(user: User): Future[Authentication[User]] = Future.successful(accept(user))
  private def rejectFuture(cause: Cause): Future[Authentication[User]] = Future.successful(reject(cause))
}

