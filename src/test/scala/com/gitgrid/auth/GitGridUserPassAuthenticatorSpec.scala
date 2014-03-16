package com.gitgrid.auth

import com.gitgrid._
import com.gitgrid.models._
import org.specs2.mutable.Specification
import reactivemongo.bson.{BSONObjectID, BSONDateTime}
import spray.routing.authentication.UserPass

class GitGridUserPassAuthenticatorSpec extends Specification with AsyncUtils {
  def newId = BSONObjectID.generate
  val now = BSONDateTime(System.currentTimeMillis)
  val yesterday = BSONDateTime(System.currentTimeMillis - 24 * 60 * 60 * 1000)
  val tomorrow = BSONDateTime(System.currentTimeMillis + 24 * 60 * 60 * 1000)

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

    "only accept most recent password" in new TestConfig {
      val db = TestDatabase.create(config)
      val upa = new GitGridUserPassAuthenticator(db)

      val u1 = await(db.users.insert(User(userName = "user1")))
      val p1a = await(db.userPasswords.insert(UserPassword(createdAt = yesterday, userId = u1.id.get, hash = "pass1-old", hashAlgorithm = "plain")))
      val p1b = await(db.userPasswords.insert(UserPassword(createdAt = now, userId = u1.id.get, hash = "pass1-new", hashAlgorithm = "plain")))
      val u2 = await(db.users.insert(User(id = Some(newId), userName = "user2")))
      val p2a = await(db.userPasswords.insert(UserPassword(createdAt = now, userId = u2.id.get, hash = "pass2-old", hashAlgorithm = "plain")))
      val p2b = await(db.userPasswords.insert(UserPassword(createdAt = tomorrow, userId = u2.id.get, hash = "pass2-new", hashAlgorithm = "plain")))

      await(upa(Some(UserPass("user1", "pass1-new")))) === Some(u1)
      await(upa(Some(UserPass("user2", "pass2-new")))) === Some(u2)
    }
  }
}
