package com.gitgrid.auth

import java.util.Date
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import com.gitgrid.http.DateJsonProtocol
import com.gitgrid.models._
import org.parboiled.common.Base64
import spray.json._

case class BearerToken(
  userId: String,
  userName: String,
  createdAt: Option[Date] = None,
  expiresAt: Option[Date] = None,
  signature: Seq[Byte] = Nil
)

object BearerTokenHandler extends DefaultJsonProtocol with DateJsonProtocol {
  implicit val tokenFormat = jsonFormat5(BearerToken)

  def generate(user: User, expiresAt: Option[Date] = None): BearerToken = {
    val now = System.currentTimeMillis
    BearerToken(
      user.id.stringify,
      user.userName,
      createdAt = Some(new Date(now)),
      expiresAt = expiresAt
    )
  }

  def calcSignature(token: BearerToken, secret: Seq[Byte]): Seq[Byte] = {
    val serialized = serialize(token.copy(signature = Nil))
    val secretKey = new SecretKeySpec(secret.toArray, "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(secretKey)
    mac.doFinal(serialized.getBytes("ASCII")).toSeq
  }

  def sign(token: BearerToken, secret: Seq[Byte]): BearerToken = token.copy(signature = calcSignature(token, secret))
  def validate(token: BearerToken, secret: Seq[Byte]): Boolean = compareConstantTime(token.signature, calcSignature(token, secret))
  def expired(token: BearerToken): Boolean = token.expiresAt.map(_.before(now)).getOrElse(false)
  def serialize(token: BearerToken): String = toBase64(token.toJson.toString)
  def deserialize(str: String): BearerToken = JsonParser(fromBase64(str)).convertTo[BearerToken]

  private lazy val base64 = Base64.rfc2045
  private def toBase64(str: String): String = base64.encodeToString(str.getBytes("UTF-8"), false)
  private def fromBase64(str: String): String = new String(base64.decode(str), "UTF-8")
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
