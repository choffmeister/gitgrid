package com.gitgrid.auth

import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import spray.json._

case class JoseHeader(
  algorithm: String
)

case class JsonWebToken(
  createdAt: Date,
  expiresAt: Date,
  subject: String,
  name: String,
  payload: Map[String, JsValue]
)

/**
 * See http://tools.ietf.org/html/draft-ietf-oauth-json-web-token-25
 */
object JsonWebToken extends JsonWebTokenProtocol {
  import com.gitgrid.utils.Base64UrlStringConverter._

  type Header = JoseHeader
  type Token = JsonWebToken
  type Signature = Array[Byte]

  implicit val joseHeaderFormat = JoseHeaderFormat
  implicit val jsonWebTokenFormat = JsonWebTokenFormat

  def createSignature(header: Header, token: Token, secret: Array[Byte]): Array[Byte] = {
    val data = jtob64(header) + "." + jtob64(token)
    header.algorithm match {
      case "HS256" => hmac("HmacSHA256", data.getBytes("UTF-8"), secret)
      case _ => throw new Exception(s"Algorithm ${header.algorithm} is not supported")
    }
  }

  def checkSignature(header: Header, token: Token, signature: Array[Byte], secret: Array[Byte]): Boolean = {
    val signature2 = createSignature(header, token, secret)
    compareConstantTime(signature, signature2)
  }

  def write(header: Header, token: Token, signature: Signature): String = {
    jtob64(header) + "." + jtob64(token) + "." + btob64(signature)
  }

  def read(tokenStr: String): Option[(Header, Token, Signature)] = {
    try {
      tokenStr.split("\\.").toList match {
        case List(s1, s2, s3) => Some((b64toj[Header](s1), b64toj[Token](s2), b64tob(s3)))
        case _ => None
      }
    } catch {
      case _: Throwable => None
    }
  }

  private def btob64(byt: Array[Byte]): String =
    bytesToBase64(byt)
  private def b64tob(b64: String): Array[Byte] =
    base64ToBytes(b64)
  private def jtob64[A](obj: A)(implicit f: RootJsonFormat[A]): String =
    stringToBase64(obj.toJson.compactPrint)
  private def b64toj[A](b64: String)(implicit f: RootJsonFormat[A]): A =
    JsonParser(base64ToString(b64)).convertTo[A]

  private def hmac(algorithm: String, data: Array[Byte], secret: Array[Byte]): Array[Byte] = {
    val hmac = Mac.getInstance(algorithm);
    val key = new SecretKeySpec(secret, algorithm);
    hmac.init(key)
    hmac.doFinal(data)
  }

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

trait JsonWebTokenProtocol extends DefaultJsonProtocol {
  implicit object JoseHeaderFormat extends RootJsonFormat[JoseHeader] {
    def write(h: JoseHeader) = JsObject(
      "typ" -> JsString("JWT"),
      "alg" -> JsString(h.algorithm)
    )
    def read(v: JsValue) = v.asJsObject.getFields("typ", "alg") match {
      case Seq(JsString(typ), JsString(alg)) if typ == "JWT" => JoseHeader(
        algorithm = alg
      )
      case _ => throw new DeserializationException("JOSE Header expected")
    }
  }

  implicit object JsonWebTokenFormat extends RootJsonFormat[JsonWebToken] {
    def write(t: JsonWebToken) = {
      val baseValues = List[(String, JsValue)](
        "ts" -> JsNumber(t.createdAt.getTime),
        "exp" -> JsNumber(t.expiresAt.getTime),
        "sub" -> JsString(t.subject),
        "name" -> JsString(t.name)
      )
      JsObject(baseValues ++ t.payload)
    }
    def read(v: JsValue) = {
      val raw = v.asJsObject
      try {
        JsonWebToken(
          createdAt = new Date(raw.fields("ts").asInstanceOf[JsNumber].value.toLong),
          expiresAt = new Date(raw.fields("exp").asInstanceOf[JsNumber].value.toLong),
          subject = raw.fields("sub").asInstanceOf[JsString].value,
          name = raw.fields("name").asInstanceOf[JsString].value,
          payload = raw.fields.filter(f => !knownClaimNames.contains(f._1))
        )
      } catch {
        case e: Throwable => throw new DeserializationException("JSON Web Token expected", e)
      }
    }

    private def knownClaimNames = List("ts", "exp", "sub", "name")
  }
}
