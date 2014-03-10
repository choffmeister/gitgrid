package com.gitgrid.auth

import org.specs2.mutable.Specification
import com.gitgrid.{TestConfig, AsyncUtils}
import com.gitgrid.models._
import reactivemongo.bson.{BSONDateTime, BSONObjectID}
import com.gitgrid.models.User
import scala.Some

class AuthenticationHandlerSpec extends Specification with AsyncUtils {
  def newId = BSONObjectID.generate
  val now = BSONDateTime(System.currentTimeMillis)
  val yesterday = BSONDateTime(System.currentTimeMillis - 24 * 60 * 60 * 1000)
  val tomorrow = BSONDateTime(System.currentTimeMillis + 24 * 60 * 60 * 1000)

  "AuthenticationHandlerSpec" should {
    "authenticate by credentials" in new TestConfig {
      val db = Database()
      val ah = new AuthenticationHandler()

      val u1 = await(db.users.insert(User(id = Some(newId), userName = "user1")))
      val p1a = await(db.userPasswords.insert(UserPassword(id = Some(newId), createdAt = yesterday, userId = u1.id.get, hash = "pass1-old", hashAlgorithm = "plain")))
      val p1b = await(db.userPasswords.insert(UserPassword(id = Some(newId), createdAt = now, userId = u1.id.get, hash = "pass1-new", hashAlgorithm = "plain")))
      val u2 = await(db.users.insert(User(id = Some(newId), userName = "user2")))
      val p2a = await(db.userPasswords.insert(UserPassword(id = Some(newId), createdAt = now, userId = u2.id.get, hash = "pass2-old", hashAlgorithm = "plain")))
      val p2b = await(db.userPasswords.insert(UserPassword(id = Some(newId), createdAt = tomorrow, userId = u2.id.get, hash = "pass2-new", hashAlgorithm = "plain")))

      await(ah.authenticate("user1", "pass1-new")) === Some(u1)
      await(ah.authenticate("user2", "pass2-new")) === Some(u2)
    }
  }
}