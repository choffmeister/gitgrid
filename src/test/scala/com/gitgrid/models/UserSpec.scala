package com.gitgrid.models

import com.gitgrid._
import org.specs2.mutable._

class UserSpec extends Specification with AsyncUtils {
  "User" should {
    "work" in new TestConfig {
      val db = TestDatabase.create(config)
      val u1 = User(userName = "user1")
      val u2 = User(userName = "user2")
      val u3 = User(userName = "user3")

      await(db.users.all) must haveSize(0)
      await(db.users.insert(u1))
      await(db.users.all) must haveSize(1)
      await(db.users.all)(0) === u1
      await(db.users.insert(u2))
      await(db.users.insert(u3))
      await(db.users.all) must haveSize(3)
      await(db.users.all).sortBy(u => u.userName) === Seq(u1, u2, u3)
      await(db.users.find(u2.id.get)) === Some(u2)
      await(db.users.delete(u2))
      await(db.users.all) must haveSize(2)
      await(db.users.find(u2.id.get)) must beNone
      await(db.users.update(u3.copy(userName = "user3-new")))
      await(db.users.find(u3.id.get)).get.userName === "user3-new"

      await(db.users.findByUserName("user1")) === Some(u1)

      ok
    }
  }
}
