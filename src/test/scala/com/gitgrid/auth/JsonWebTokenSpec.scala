package com.gitgrid.auth

import java.util.Date

import org.specs2.mutable.Specification
import spray.json._

class JsonWebTokenSpec extends Specification {
  def now = time(0)
  def time(delta: Long = 0) = new Date(System.currentTimeMillis + delta)

  "JsonWebToken" should {
    "work" in {
      val jh = JoseHeader(algorithm = "HS256")
      val t1 = JsonWebToken(createdAt = now, expiresAt = time(60 * 1000), subject = "me", Map("foo" -> JsString("bar"), "apple" -> JsNumber(1)))
      val sig = JsonWebToken.createSignature(jh, t1, "secret".getBytes("UTF-8"))
      val s1 = JsonWebToken.write(jh, t1, sig)
      val t2 = JsonWebToken.read(s1).get

      t1 === t2._2
      JsonWebToken.checkSignature(t2._1, t2._2, sig, "secret".getBytes("UTF-8")) === true
    }
  }
}
