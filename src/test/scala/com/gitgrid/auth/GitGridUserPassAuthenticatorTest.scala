package com.gitgrid.auth

import com.gitgrid._
import org.specs2.mutable.Specification
import spray.routing.authentication.UserPass

class GitGridUserPassAuthenticatorTest extends Specification with AsyncUtils {
  "GitGridUserPassAuthenticator" should {
    "properly authenticate user passed" in new TestDatabase {
      val upa = new GitGridUserPassAuthenticator(db)

      await(upa(None)) must beNone
      await(upa(Some(UserPass("user1", "pass1")))) must beSome(user1)
      await(upa(Some(UserPass("user2", "pass2")))) must beSome(user2)
      await(upa(Some(UserPass("user1", "pass2")))) must beNone
      await(upa(Some(UserPass("user2", "pass1")))) must beNone
      await(upa(Some(UserPass("user", "pass")))) must beNone
    }

    "properly check passwords hashed with plain" in new TestDatabase {
      val upa = new GitGridUserPassAuthenticator(db)

      upa.checkPassword("", "", "plain", "") === true
      upa.checkPassword("a", "", "plain", "a") === true
      upa.checkPassword("ab", "", "plain", "ab") === true
      upa.checkPassword("abc", "", "plain", "abc") === true

      upa.checkPassword("", "", "plain", "a") === false
      upa.checkPassword("a", "", "plain", "ab") === false
      upa.checkPassword("ab", "", "plain", "abc") === false
      upa.checkPassword("abc", "", "plain", "abC") === false
    }
  }
}
