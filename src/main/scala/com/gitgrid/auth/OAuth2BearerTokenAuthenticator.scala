package com.gitgrid.auth

import java.util.Date

import spray.http.HttpHeaders._
import spray.http._
import spray.json._
import spray.routing.AuthenticationFailedRejection.{CredentialsMissing => Missing, CredentialsRejected => Rejected}
import spray.routing.{AuthenticationFailedRejection => AuthRejection, _}
import spray.util._

import scala.concurrent._

/**
 * See http://tools.ietf.org/html/rfc6749
 */
case class OAuth2AccessTokenResponse(tokenType: String, accessToken: String, expiresIn: Long)

/**
 * See http://tools.ietf.org/html/rfc6750
 */
class OAuth2BearerTokenAuthenticator[U](val realm: String, val serverSecret: Array[Byte], user: String => Future[Option[U]])
    (implicit val executionContext: ExecutionContext, val tokenPayloadFormat: JsonFormat[U]) extends EnhancedHttpAuthenticator[U] {
  import com.gitgrid.auth.OAuth2BearerTokenAuthenticator._

  def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext): EnhancedAuthentication[U] = {
    extractToken(credentials) match {
      case Left(r) => future(Left(r))
      case Right((header, token, signature)) =>
        if (!JsonWebToken.checkSignature(header, token, signature, serverSecret)) future(Left(createRejection(TokenManipulated)))
        else if (token.expiresAt.before(now)) future(Left(createRejection(TokenExpired)))
        else user(token.subject).map {
          case Some(u) => Right(u)
          case None => Left(createRejection(TokenSubjectRejected))
        }
    }
  }

  def challenges(error: Error): List[HttpHeader] = {
    val desc = error match {
      case TokenMissing => None
      case TokenMalformed => Some("The access token is malformed")
      case TokenManipulated => Some("The access token has been manipulated")
      case TokenExpired => Some("The access token expired")
      case TokenSubjectRejected => Some("The access token subject has been rejected")
      case _ => Some("An unknown error occured")
    }
    val params = desc match {
      case Some(desc) => Map("error" -> "invalid_token", "error_description" -> desc)
      case None => Map.empty[String, String]
    }
    `WWW-Authenticate`(HttpChallenge(scheme = "Bearer", realm = realm, params = params)) :: Nil
  }

  def extractToken(credentials: Option[HttpCredentials]): Either[AuthRejection, (JoseHeader, JsonWebToken, Array[Byte])] = {
    credentials match {
      case Some(OAuth2BearerToken(tokenStr)) =>
        try {
          JsonWebToken.read(tokenStr) match {
            case Some((h, t, s)) => Right((h, t, s))
            case _ => Left(createRejection(TokenMalformed))
          }
        } catch {
          case _: Throwable => Left(createRejection(TokenMalformed))
        }
      case _ => Left(createRejection(TokenMissing))
    }
  }

  def extractToken(req: HttpRequest): Either[AuthRejection, (JoseHeader, JsonWebToken, Array[Byte])] = {
    val authHeader = req.headers.findByType[`Authorization`]
    val credentials = authHeader.map { case Authorization(c) ⇒ c }
    extractToken(credentials)
  }

  def createRejection(error: Error): AuthRejection = error match {
    case TokenMissing => AuthRejection(Missing, challenges(error))
    case _ => AuthRejection(Rejected, challenges(error))
  }

  private def now: Date = new Date(System.currentTimeMillis)
}

object OAuth2BearerTokenAuthenticator {
  abstract sealed class Error
  case object TokenMalformed extends Error
  case object TokenManipulated extends Error
  case object TokenExpired extends Error
  case object TokenMissing extends Error
  case object TokenSubjectRejected extends Error
}
