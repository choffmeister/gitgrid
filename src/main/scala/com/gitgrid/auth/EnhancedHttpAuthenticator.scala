package com.gitgrid.auth

import spray.http.HttpHeaders.{Authorization, `WWW-Authenticate`}
import spray.http._
import spray.routing.AuthenticationFailedRejection.{CredentialsMissing => Missing, CredentialsRejected => Rejected}
import spray.routing.authentication._
import spray.routing.{RequestContext, AuthenticationFailedRejection => AuthRejection}
import spray.util._

import scala.concurrent._

trait EnhancedHttpAuthenticator[U] extends ContextAuthenticator[U] {
  type EnhancedAuthentication[U] = Future[Either[AuthRejection, U]]
  implicit val executionContext: ExecutionContext

  def apply(ctx: RequestContext): Future[Authentication[U]] = {
    val authHeader = ctx.request.headers.findByType[`Authorization`]
    val credentials = authHeader.map { case Authorization(c) ⇒ c }
    authenticate(credentials, ctx) map {
      case Right(u) => Right(u)
      case Left(r) => Left(r)
    }
  }

  def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext): EnhancedAuthentication[U]
}

object EnhancedHttpAuthenticator {
  def map[U, V](a: EnhancedHttpAuthenticator[U], transform: U => V) = new EnhancedHttpAuthenticator[V] {
    override implicit val executionContext: ExecutionContext = a.executionContext

    override def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext) = {
      a.authenticate(credentials, ctx).map {
        case Right(u) => Right(transform(u))
        case Left(r) => Left(r)
      }
    }
  }

  def combine[U](a1: EnhancedHttpAuthenticator[U], a2: EnhancedHttpAuthenticator[U]) = new EnhancedHttpAuthenticator[U] {
    override implicit val executionContext: ExecutionContext = a1.executionContext

    override def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext) = {
      a1.authenticate(credentials, ctx).flatMap {
        case Right(u1) => future(Right(u1))
        case Left(r1) => a2.authenticate(credentials, ctx).map {
          case Right(u2) => Right(u2)
          case Left(r2) =>
            (r1, r2) match {
              case (AuthRejection(Missing, c1), AuthRejection(Missing, c2)) =>
                Left(AuthRejection(Missing, c1 ++ c2))
              case (AuthRejection(_, c1), AuthRejection(_, c2)) =>
                Left(AuthRejection(Rejected, c1 ++ c2))
            }
        }
      }
    }
  }
}

class EnhancedBasicHttpAuthenticator[U](val realm: String, val userPassAuthenticator: UserPassAuthenticator[U])(implicit val executionContext: ExecutionContext) extends EnhancedHttpAuthenticator[U] {
  override def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext): EnhancedAuthentication[U] = {
    credentials match {
      case Some(BasicHttpCredentials(u, p)) =>
        userPassAuthenticator(Some(UserPass(u, p))).map {
          case Some(user) => Right(user)
          case _ => Left(AuthRejection(Rejected, challenges))
        }
      case _ =>
        future(Left(AuthRejection(Missing, challenges)))
    }
  }

  def challenges = `WWW-Authenticate`(HttpChallenge(scheme = "Basic", realm = realm, params = Map.empty)) :: Nil
}

