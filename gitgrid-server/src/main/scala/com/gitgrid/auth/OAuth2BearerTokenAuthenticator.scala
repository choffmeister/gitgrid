package com.gitgrid.auth

import java.util.Date

import spray.http.HttpHeaders._
import spray.http._
import spray.json._
import spray.routing.AuthenticationFailedRejection.{CredentialsMissing => Missing, CredentialsRejected => Rejected}
import spray.routing.authentication._
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
    (implicit val executionContext: ExecutionContext, val tokenPayloadFormat: JsonFormat[U]) extends HttpAuthenticator[U] {
  import com.gitgrid.auth.OAuth2BearerTokenAuthenticator._

  override def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext): Future[Option[U]] = {
    credentials match {
      case Some(t: OAuth2BearerToken) => extractToken(t) match {
        case Right((header, token, signature)) =>
          if (!JsonWebToken.checkSignature(header, token, signature, serverSecret)) future(None)
          else if (token.expiresAt.before(now)) future(None)
          else user(token.subject)
        case Left(err) => future(None)
      }
      case _ => future(None)
    }
  }

  override def getChallengeHeaders(httpRequest: HttpRequest): List[HttpHeader] = extractToken(httpRequest) match {
    case Right((header, token, signature)) =>
      if (!JsonWebToken.checkSignature(header, token, signature, serverSecret)) getChallengeHeaders(TokenManipulated)
      else if (token.expiresAt.before(now)) getChallengeHeaders(TokenExpired)
      else getChallengeHeaders(TokenSubjectRejected)
    case Left(error) => getChallengeHeaders(error)
  }

  def getChallengeHeaders(error: Error): List[HttpHeader] = {
    val desc = error match {
      case TokenMissing => None
      case TokenMalformed => Some("The access token is malformed")
      case TokenManipulated => Some("The access token has been manipulated")
      case TokenExpired => Some("The access token expired")
      case TokenSubjectRejected => Some("The access token subject has been rejected")
      case _ => Some("An unknown error occured")
    }
    val params = desc match {
      case Some(msg) => Map("error" -> "invalid_token", "error_description" -> msg)
      case None => Map.empty[String, String]
    }
    `WWW-Authenticate`(HttpChallenge(scheme = "Bearer", realm = realm, params = params)) :: Nil
  }

  def extractToken(token: OAuth2BearerToken): Either[Error, (JoseHeader, JsonWebToken, Array[Byte])] = {
    try {
      JsonWebToken.read(token.token) match {
        case Some((h, t, s)) => Right((h, t, s))
        case _ => Left(TokenMalformed)
      }
    } catch {
      case _: Throwable => Left(TokenMalformed)
    }
  }

  def extractToken(req: HttpRequest): Either[Error, (JoseHeader, JsonWebToken, Array[Byte])] = {
    val authHeader = req.headers.findByType[`Authorization`]
    val credentials = authHeader.map { case Authorization(creds) ⇒ creds }
    credentials match {
      case Some(t: OAuth2BearerToken) => extractToken(t)
      case _ => Left(TokenMissing)
    }
  }

  def createRejection(error: Error): AuthRejection = error match {
    case TokenMissing => AuthRejection(Missing, getChallengeHeaders(error))
    case _ => AuthRejection(Rejected, getChallengeHeaders(error))
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
