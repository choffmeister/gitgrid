package com.gitgrid.auth

import spray.http.HttpHeaders.{Authorization, `WWW-Authenticate`}
import spray.http._
import spray.routing.AuthenticationFailedRejection.{CredentialsMissing => Missing, CredentialsRejected => Rejected}
import spray.routing.authentication._
import spray.routing.{RequestContext, AuthenticationFailedRejection => AuthRejection}
import spray.util._

import scala.concurrent._

class BasicHttpAuthenticator[U](val realm: String, val userPassAuthenticator: UserPassAuthenticator[U])(implicit val executionContext: ExecutionContext) extends EnhancedHttpAuthenticator[U] {
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

