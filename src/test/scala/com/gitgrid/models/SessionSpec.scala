package com.gitgrid.models

import com.gitgrid._
import org.specs2.mutable._
import reactivemongo.bson.BSONObjectID

class SessionSpec extends Specification with AsyncUtils {
  def newId = BSONObjectID.generate

  "Session" should {
    "work" in new TestConfig {
      val db = TestDatabase.create(config)
      val s1 = Session(userId = newId, sessionId = "session1")
      val s2 = Session(userId = newId, sessionId = "session2")
      val s3 = Session(userId = newId, sessionId = "session3")

      await(db.sessions.all) must haveSize(0)
      await(db.sessions.insert(s1))
      await(db.sessions.all) must haveSize(1)
      await(db.sessions.all)(0) === s1
      await(db.sessions.insert(s2))
      await(db.sessions.insert(s3))
      await(db.sessions.all) must haveSize(3)
      await(db.sessions.all).sortBy(u => u.sessionId) === Seq(s1, s2, s3)
      await(db.sessions.find(s2.id.get)) === Some(s2)
      await(db.sessions.delete(s2))
      await(db.sessions.all) must haveSize(2)
      await(db.sessions.find(s2.id.get)) must beNone
      await(db.sessions.update(s3.copy(sessionId = "session3-new")))
      await(db.sessions.find(s3.id.get)).get.sessionId === "session3-new"

      ok
    }
  }
}
