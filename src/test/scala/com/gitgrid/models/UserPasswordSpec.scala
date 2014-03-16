package com.gitgrid.models

import com.gitgrid._
import org.specs2.mutable._
import reactivemongo.bson.{BSONDateTime, BSONObjectID}

class UserPasswordSpec extends Specification with AsyncUtils {
  def newId = BSONObjectID.generate

  "UserPassword" should {
    "work" in new TestConfig {
      val db = TestDatabase.create(config)
      val up1 = UserPassword(userId = newId, createdAt = BSONDateTime(0))
      val up2 = UserPassword(userId = newId, createdAt = BSONDateTime(1))
      val up3 = UserPassword(userId = newId, createdAt = BSONDateTime(2))

      await(db.userPasswords.all) must haveSize(0)
      await(db.userPasswords.insert(up1))
      await(db.userPasswords.all) must haveSize(1)
      await(db.userPasswords.all)(0) === up1
      await(db.userPasswords.insert(up2))
      await(db.userPasswords.insert(up3))
      await(db.userPasswords.all) must haveSize(3)
      await(db.userPasswords.all).sortBy(u => u.createdAt.value) === Seq(up1, up2, up3)
      await(db.userPasswords.find(up2.id.get)) === Some(up2)
      await(db.userPasswords.delete(up2))
      await(db.userPasswords.all) must haveSize(2)
      await(db.userPasswords.find(up2.id.get)) must beNone
      await(db.userPasswords.update(up3.copy(createdAt = BSONDateTime(4))))
      await(db.userPasswords.find(up3.id.get)).get.createdAt === BSONDateTime(4)

      val up4 = UserPassword(id = Some(newId), userId = up3.id.get, createdAt = BSONDateTime(5))
      await(db.userPasswords.insert(up4))
      await(db.userPasswords.findCurrentPassword(up3.id.get)) === Some(up4)

      ok
    }
  }
}
