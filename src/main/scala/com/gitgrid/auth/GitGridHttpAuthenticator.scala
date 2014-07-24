package com.gitgrid.auth

import com.gitgrid.Config
import com.gitgrid.managers._
import com.gitgrid.models._
import reactivemongo.bson._
import scala.concurrent._
import spray.http.HttpHeader
import spray.routing.AuthenticationFailedRejection._
import spray.routing._
import spray.routing.authentication._

class GitGridHttpAuthenticator(cfg: Config, db: Database)(implicit ec: ExecutionContext) extends ContextAuthenticator[User] {
  val userManager = new UserManager(db)
  val userPassAuthenticator = new GitGridUserPassAuthenticator(userManager)
  val basicAuthenticator = new BasicHttpAuthenticator[User](cfg.httpAuthBasicRealm, userPassAuthenticator)

  def apply(ctx: RequestContext): Future[Authentication[User]] = {
    authenticateByBearerToken(ctx).flatMap {
      case Right(user) => acceptF(user)
      case _ => authenticateByBasicHttp(ctx)
    }
  }

  def authenticateByBearerToken(ctx: RequestContext): Future[Authentication[User]] = {
    ctx.request.headers.find(_.is("authorization")) match {
      case Some(h) if h.value.toLowerCase.startsWith("bearer ") =>
        val token = BearerTokenHandler.deserialize(h.value.substring(7))
        val valid = BearerTokenHandler.validate(token, cfg.httpAuthBearerTokenServerSecret)
        if (!valid) rejectF(CredentialsRejected, Nil)
        else db.users.find(BSONObjectID(token.userId)).map {
          case Some(user) => accept(user)
          case _ => reject(CredentialsRejected, Nil)
        }
      case _ => rejectF(CredentialsMissing, Nil)
    }
  }

  def authenticateByBasicHttp(ctx: RequestContext): Future[Authentication[User]] = {
    basicAuthenticator(ctx)
  }

  private def accept(u: User): Authentication[User] = Right[Rejection, User](u)
  private def reject(c: Cause, ch: List[HttpHeader]): Authentication[User] = Left[Rejection, User](AuthenticationFailedRejection(c, ch))
  private def acceptF(u: User): Future[Authentication[User]] = Future.successful(accept(u))
  private def rejectF(c: Cause, ch: List[HttpHeader]): Future[Authentication[User]] = Future.successful(reject(c, ch))
}

