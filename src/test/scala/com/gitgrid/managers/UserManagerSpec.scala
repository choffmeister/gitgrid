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
  "UserManager" should {
    "set and validate password" in new EmptyTestEnvironment {
      val u1 = await(db.users.insert(User(userName = "user1")))
      val u2 = await(db.users.insert(User(userName = "user2")))

      await(um.changeUserPassword(u1, "pass1-old"))
      await(um.changeUserPassword(u2, "pass2-old"))
      Thread.sleep(100L)
      await(um.validateUserPassword(u1, "pass1-old")) === true
      await(um.validateUserPassword(u2, "pass2-old")) === true
      await(um.validateUserPassword(u1, "pass1-new")) === false
      await(um.validateUserPassword(u2, "pass2-new")) === false

      await(um.changeUserPassword(u1, "pass1-new"))
      Thread.sleep(100L)
      await(um.validateUserPassword(u1, "pass1-old")) === false
      await(um.validateUserPassword(u2, "pass2-old")) === true
      await(um.validateUserPassword(u1, "pass1-new")) === true
      await(um.validateUserPassword(u2, "pass2-new")) === false

      await(um.changeUserPassword(u2, "pass2-new"))
      Thread.sleep(100L)
      await(um.validateUserPassword(u1, "pass1-old")) === false
      await(um.validateUserPassword(u2, "pass2-old")) === false
      await(um.validateUserPassword(u1, "pass1-new")) === true
      await(um.validateUserPassword(u2, "pass2-new")) === true
    }

    "authenticate users" in new TestEnvironment {
      await(um.authenticateUser("user1", "pass1")) === Some(user1)
      await(um.authenticateUser("user2", "pass2")) === Some(user2)
      await(um.authenticateUser("user1", "pass2")) === None
      await(um.authenticateUser("user2", "pass1")) === None
    }
  }
}
