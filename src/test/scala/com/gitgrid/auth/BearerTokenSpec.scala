package com.gitgrid.auth

import com.gitgrid._
import com.gitgrid.models.User
import org.specs2.mutable.Specification

class BearerTokenSpec extends Specification {
  import BearerTokenHandler._
  val user1 = User(userName = "user1")

  "BearerToken" should {
    "fail if secret is changed" in {
      val s1 = Array[Byte](0, 1, 2)
      val s2 = Array[Byte](0, 1, 3)
      val t1 = sign(generate(user1), s1)
      println(serialize(t1))
      val t2 = deserialize(serialize(t1))
      validate(t2, s2) === false
    }

    "recognize un- and manipulated tokens" in {
      val s = Array[Byte](0, 1, 2)
      val t1 = sign(generate(user1), s)
      val t2a = deserialize(serialize(t1))
      val t2b = t1.copy(userName = "user2")
      validate(t2a, s) === true
      validate(t2b, s) === false
    }
  }
}
