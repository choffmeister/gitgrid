package com.gitgrid.auth

import spray.http.HttpHeaders.{Authorization, `WWW-Authenticate`}
import spray.http._
import spray.routing.AuthenticationFailedRejection.{CredentialsMissing => Missing, CredentialsRejected => Rejected}
import spray.routing.authentication._
import spray.routing.{RequestContext, AuthenticationFailedRejection => AuthRejection}
import spray.util._

import scala.concurrent._

trait EnhancedHttpAuthenticator[U] extends ContextAuthenticator[U] { self =>
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

  def map[V](f: U => V) = new EnhancedHttpAuthenticator[V] {
    override implicit val executionContext: ExecutionContext = self.executionContext

    override def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext) = {
      self.authenticate(credentials, ctx).map {
        case Right(u) => Right(f(u))
        case Left(r) => Left(r)
      }
    }
  }

  def andThen(other: EnhancedHttpAuthenticator[U]) = new EnhancedHttpAuthenticator[U] {
    override implicit val executionContext: ExecutionContext = self.executionContext

    override def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext) = {
      self.authenticate(credentials, ctx).flatMap {
        case Right(u1) => future(Right(u1))
        case Left(r1) => other.authenticate(credentials, ctx).map {
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
