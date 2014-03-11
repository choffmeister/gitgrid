package com.gitgrid

import com.gitgrid.models._
import reactivemongo.bson._

trait TestDatabase extends TestConfig with AsyncUtils {
  def newId = BSONObjectID.generate

  val db = Database()

  val user1 = await(db.users.insert(User(id = Some(newId), userName = "user1")))
  val password1 = await(db.userPasswords.insert(UserPassword(id = Some(newId), userId = user1.id.get, createdAt = BSONDateTime(System.currentTimeMillis), hash = "pass1", hashAlgorithm = "plain")))
  val user2 = await(db.users.insert(User(id = Some(newId), userName = "user2")))
  val password2 = await(db.userPasswords.insert(UserPassword(id = Some(newId), userId = user2.id.get, createdAt = BSONDateTime(System.currentTimeMillis), hash = "pass2", hashAlgorithm = "plain")))
}
