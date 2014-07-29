package com.gitgrid.auth

import spray.http.HttpHeaders.{Authorization, `WWW-Authenticate`}
import spray.http._
import spray.routing.authentication._
import spray.routing.{AuthenticationFailedRejection, RequestContext}
import spray.util._

import scala.concurrent._

trait EnhancedHttpAuthenticator[U] extends ContextAuthenticator[U] {
  type EnhancedAuthentication[U] = Future[Either[(AuthenticationFailedRejection.Cause, List[HttpChallenge]), U]]
  implicit val executionContext: ExecutionContext

  def apply(ctx: RequestContext): Future[Authentication[U]] = {
    val authHeader = ctx.request.headers.findByType[`Authorization`]
    val credentials = authHeader.map { case Authorization(c) ⇒ c }
    authenticate(credentials, ctx) map {
      case Right(user) => Right(user)
      case Left((cause, challenges)) => Left(AuthenticationFailedRejection(cause, challenges.map(`WWW-Authenticate`(_))))
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
            val cause = if (r1._1 == AuthenticationFailedRejection.CredentialsRejected) r1._1 else r2._1
            Left(cause, r1._2 ++ r2._2)
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
          case _ => Left((AuthenticationFailedRejection.CredentialsRejected, challenges))
        }
      case _ =>
        future(Left((AuthenticationFailedRejection.CredentialsMissing, challenges)))
    }
  }

  def challenges = HttpChallenge(scheme = "Basic", realm = realm, params = Map.empty) :: Nil
}

