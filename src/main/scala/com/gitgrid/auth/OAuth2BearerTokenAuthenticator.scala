package com.gitgrid.auth

import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import spray.http.HttpHeaders._
import spray.http._
import spray.json._
import spray.routing._
import spray.util._

import scala.concurrent._

case class OAuth2BearerTokenTyped[A](
    payload: A,
    createdAt: Date,
    expiresAt: Date,
    signature: Seq[Byte] = Nil) {
  def sign(secret: Seq[Byte])(implicit payloadFormat: JsonFormat[A]): OAuth2BearerTokenTyped[A] = copy(signature = calcSignature(secret))
  def validate(secret: Seq[Byte])(implicit payloadFormat: JsonFormat[A]): Boolean = compareConstantTime(signature, calcSignature(secret))
  def expired: Boolean = expiresAt.before(now)

  def calcSignature(secret: Seq[Byte])(implicit payloadFormat: JsonFormat[A]): Seq[Byte] = {
    val serialized = OAuth2BearerTokenSerializer.serialize(copy(signature = Nil))
    val secretKey = new SecretKeySpec(secret.toArray, "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(secretKey)
    mac.doFinal(serialized.getBytes("ASCII")).toSeq
  }

  private def now: Date = new Date(System.currentTimeMillis)
  private def compareConstantTime[T](s1: Seq[T], s2: Seq[T]): Boolean = {
    var res = true
    val l = Math.max(s1.length, s2.length)
    for (i <- 0 until l) {
      if (s1.length <= i || s2.length <= i || s1(i) != s2(i)) {
        res = false
      }
    }
    res
  }
}

object OAuth2BearerTokenTyped {
  def create[A](payload: A, expiresAt: Date) =
    OAuth2BearerTokenTyped[A](payload, new Date(System.currentTimeMillis), expiresAt, Seq.empty[Byte])
}

object OAuth2BearerTokenSerializer extends DefaultJsonProtocol {
  import com.gitgrid.utils.BinaryStringConverter._

  private class ContainerFormat[A](implicit payloadFormat: JsonFormat[A]) extends JsonFormat[OAuth2BearerTokenTyped[A]] {
    def write(token: OAuth2BearerTokenTyped[A]) = JsObject(
      "ts" -> JsNumber(token.createdAt.getTime),
      "exp" -> JsNumber(token.expiresAt.getTime),
      "data" -> token.payload.toJson,
      "sig" -> JsString(bytes2hex(token.signature.toArray))
    )
    def read(value: JsValue) =
      value.asJsObject.getFields("ts", "exp", "data", "sig") match {
        case Seq(JsNumber(ts), JsNumber(exp), data@JsObject(_), JsString(sig)) =>
          OAuth2BearerTokenTyped[A](data.convertTo[A], new Date(ts.toLong), new Date(exp.toLong), hex2bytes(sig).toSeq)
        case _ => throw new DeserializationException("Color expected")
      }
  }

  def serialize[A](token: OAuth2BearerTokenTyped[A])(implicit payloadFormat: JsonFormat[A]): String = {
    implicit val format = new ContainerFormat[A]
    stringToBase64(token.toJson.toString)
  }
  def deserialize[A](str: String)(implicit payloadFormat: JsonFormat[A]): OAuth2BearerTokenTyped[A] = {
    implicit val format = new ContainerFormat[A]
    JsonParser(base64ToString(str)).convertTo[OAuth2BearerTokenTyped[A]]
  }
}

/**
 * See http://tools.ietf.org/html/rfc6750.
 */
class OAuth2BearerTokenAuthenticator[U](val realm: String, val serverSecret: Seq[Byte])
    (implicit val executionContext: ExecutionContext, val tokenPayloadFormat: JsonFormat[U]) extends EnhancedHttpAuthenticator[U] {
  def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext): EnhancedAuthentication[U] = {
    extractToken(credentials) match {
      case Left(TokenMissing) => future(Left(AuthenticationFailedRejection.CredentialsMissing, challenges(TokenMissing)))
      case Left(error) => future(Left(AuthenticationFailedRejection.CredentialsRejected, challenges(error)))
      case Right(token) =>
        if (!token.validate(serverSecret)) future(Left(AuthenticationFailedRejection.CredentialsRejected, challenges(TokenManipulated)))
        else if (token.expired) future(Left(AuthenticationFailedRejection.CredentialsRejected, challenges(TokenExpired)))
        else future(Right(token.payload))
    }
  }

  def challenges(error: Error): List[HttpChallenge] = {
    val desc = error match {
      case TokenMalformed => Some("The access token is malformed")
      case TokenManipulated => Some("The access token has been manipulated")
      case TokenExpired => Some("The access token expired")
      case TokenMissing => None
      case _ => Some("An unknown error occured")
    }
    val params = desc match {
      case Some(desc) => Map("error" -> "invalid_token", "error_description" -> desc)
      case None => Map.empty[String, String]
    }
    HttpChallenge(scheme = "Bearer", realm = realm, params = params) :: Nil
  }

  def extractToken(credentials: Option[HttpCredentials]): Either[Error, OAuth2BearerTokenTyped[U]] = {
    credentials match {
      case Some(OAuth2BearerToken(tokenStr)) =>
        try {
          Right(OAuth2BearerTokenSerializer.deserialize[U](tokenStr))
        } catch {
          case _: Throwable => Left(TokenMalformed)
        }
      case _ => Left(TokenMissing)
    }
  }

  def extractToken(req: HttpRequest): Either[Error, OAuth2BearerTokenTyped[U]] = {
    val authHeader = req.headers.findByType[`Authorization`]
    val credentials = authHeader.map { case Authorization(c) ⇒ c }
    extractToken(credentials)
  }

  abstract sealed class Error
  case object TokenMalformed extends Error
  case object TokenManipulated extends Error
  case object TokenExpired extends Error
  case object TokenMissing extends Error
}
