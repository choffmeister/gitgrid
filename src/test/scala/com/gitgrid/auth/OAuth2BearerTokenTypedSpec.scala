package com.gitgrid.auth

import java.util.Date

import org.specs2.mutable.Specification
import spray.json.DefaultJsonProtocol

class OAuth2BearerTokenTypedSpec extends Specification with DefaultJsonProtocol {
  case class TokenPayload(userName: String, age: Int)
  implicit val payloadFormat = jsonFormat2(TokenPayload)

  "OAuth2BearerTokenTyped" should {
    val tomorrow = new Date(System.currentTimeMillis + 24 * 60 * 60 * 1000)

    "serialize and deserialize tokens" in {
      val s = Array[Byte](0, 1, 2)
      val t1 = OAuth2BearerTokenTyped.create(TokenPayload("user1", 1), tomorrow).sign(s)
      val ts = OAuth2BearerTokenSerializer.serialize(t1)
      val t2 = OAuth2BearerTokenSerializer.deserialize(ts)
      t1 === t2
    }
    "fail if secret is changed" in {
      val s1 = Array[Byte](0, 1, 2)
      val s2 = Array[Byte](0, 1, 3)
      OAuth2BearerTokenTyped.create(TokenPayload("user1", 1), tomorrow).sign(s1).validate(s2) === false
    }

    "recognize un- and manipulated tokens" in {
      val s = Array[Byte](0, 1, 2)
      val t1 = OAuth2BearerTokenTyped.create(TokenPayload("user1", 1), tomorrow).sign(s)
      val t2 = t1.copy(payload = TokenPayload("user2", 1))
      t1.validate(s) === true
      t2.validate(s) === false
    }
  }
}
