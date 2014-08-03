package com.gitgrid.auth

import java.util.Date

import spray.http.HttpHeaders._
import spray.http._
import spray.json._
import spray.routing._
import spray.util._

import scala.concurrent._

/**
 * See http://tools.ietf.org/html/rfc6750.
 */
class OAuth2BearerTokenAuthenticator[U](val realm: String, val serverSecret: Array[Byte], user: String => Future[Option[U]])
    (implicit val executionContext: ExecutionContext, val tokenPayloadFormat: JsonFormat[U]) extends EnhancedHttpAuthenticator[U] {
  def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext): EnhancedAuthentication[U] = {
    extractToken(credentials) match {
      case Left(TokenMissing) => future(Left(AuthenticationFailedRejection.CredentialsMissing, challenges(TokenMissing)))
      case Left(error) => future(Left(AuthenticationFailedRejection.CredentialsRejected, challenges(error)))
      case Right((header, token, signature)) =>
        if (!JsonWebToken.checkSignature(header, token, signature, serverSecret)) future(Left(AuthenticationFailedRejection.CredentialsRejected, challenges(TokenManipulated)))
        else if (token.expiresAt.before(now)) future(Left(AuthenticationFailedRejection.CredentialsRejected, challenges(TokenExpired)))
        else user(token.subject).map {
          case Some(u) => Right(u)
          case None => Left(AuthenticationFailedRejection.CredentialsRejected, challenges(TokenSubjectRejected))
        }
    }
  }

  def challenges(error: Error): List[HttpChallenge] = {
    val desc = error match {
      case TokenMalformed => Some("The access token is malformed")
      case TokenManipulated => Some("The access token has been manipulated")
      case TokenExpired => Some("The access token expired")
      case TokenSubjectRejected => Some("The access token subject has been rejected")
      case TokenMissing => None
      case _ => Some("An unknown error occured")
    }
    val params = desc match {
      case Some(desc) => Map("error" -> "invalid_token", "error_description" -> desc)
      case None => Map.empty[String, String]
    }
    HttpChallenge(scheme = "Bearer", realm = realm, params = params) :: Nil
  }

  def extractToken(credentials: Option[HttpCredentials]): Either[Error, (JoseHeader, JsonWebToken, Array[Byte])] = {
    credentials match {
      case Some(OAuth2BearerToken(tokenStr)) =>
        try {
          JsonWebToken.read(tokenStr) match {
            case Some((h, t, s)) => Right((h, t, s))
            case _ => Left(TokenMalformed)
          }
        } catch {
          case _: Throwable => Left(TokenMalformed)
        }
      case _ => Left(TokenMissing)
    }
  }

  def extractToken(req: HttpRequest): Either[Error, (JoseHeader, JsonWebToken, Array[Byte])] = {
    val authHeader = req.headers.findByType[`Authorization`]
    val credentials = authHeader.map { case Authorization(c) ⇒ c }
    extractToken(credentials)
  }

  private def now: Date = new Date(System.currentTimeMillis)

  abstract sealed class Error
  case object TokenMalformed extends Error
  case object TokenManipulated extends Error
  case object TokenExpired extends Error
  case object TokenMissing extends Error
  case object TokenSubjectRejected extends Error
}
