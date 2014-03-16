package com.gitgrid.managers

import com.gitgrid._
import com.gitgrid.auth.GitGridUserPassAuthenticator
import com.gitgrid.models.User
import com.gitgrid.models.UserPassword
import org.specs2.mutable._
import reactivemongo.bson.BSONDateTime
import scala.Some
import spray.routing.authentication.UserPass

class UserManagerSpec extends Specification with AsyncUtils {
  val now = BSONDateTime(System.currentTimeMillis)
  val yesterday = BSONDateTime(System.currentTimeMillis - 24 * 60 * 60 * 1000)
  val tomorrow = BSONDateTime(System.currentTimeMillis + 24 * 60 * 60 * 1000)

  "UserManager" should {
    "set password" in {
      val db = TestDatabase.create()
      val um = new UserManager(db)
      val upa = new GitGridUserPassAuthenticator(db)

      val u1 = await(db.users.insert(User(userName = "user1")))
      val p1 = await(db.userPasswords.insert(UserPassword(createdAt = yesterday, userId = u1.id.get, hash = "pass1-old", hashAlgorithm = "plain")))

      await(upa(Some(UserPass("user1", "pass1-old")))) must beSome(u1)
      await(upa(Some(UserPass("user1", "pass2-new")))) must beNone
      await(um.setPassword(u1, "pass2-new"))
      await(upa(Some(UserPass("user1", "pass1-old")))) must beNone
      await(upa(Some(UserPass("user1", "pass2-new")))) must beSome(u1)
    }
  }
}
