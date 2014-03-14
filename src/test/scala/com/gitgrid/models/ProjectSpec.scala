package com.gitgrid.models

import com.gitgrid._
import org.specs2.mutable._
import reactivemongo.bson.BSONObjectID

class ProjectSpec extends Specification with AsyncUtils {
  def newId = BSONObjectID.generate

  "User" should {
    "work" in new TestConfig {
      val db = Database()
      val p1 = Project(userId = newId, canonicalName = "p1")
      val p2 = Project(userId = newId, canonicalName = "p2")
      val p3 = Project(userId = newId, canonicalName = "p3")

      await(db.projects.all) must haveSize(0)
      await(db.projects.insert(p1))
      await(db.projects.all) must haveSize(1)
      await(db.projects.all)(0) === p1
      await(db.projects.insert(p2))
      await(db.projects.insert(p3))
      await(db.projects.all) must haveSize(3)
      await(db.projects.all).sortBy(u => u.canonicalName) === Seq(p1, p2, p3)
      await(db.projects.find(p2.id.get)) === Some(p2)
      await(db.projects.delete(p2))
      await(db.projects.all) must haveSize(2)
      await(db.projects.find(p2.id.get)) must beNone
      await(db.projects.update(p3.copy(canonicalName = "p3-new")))
      await(db.projects.find(p3.id.get)).get.canonicalName === "p3-new"

      ok
    }
  }
}
