package com.gitgrid.models

import com.gitgrid._
import org.specs2.mutable._
import reactivemongo.bson.BSONObjectID

class ProjectSpec extends Specification with AsyncUtils {
  def newId = BSONObjectID.generate

  "User" should {
    "work" in new EmptyTestEnvironment {
      val p1 = Project(userId = newId, name = "p1")
      val p2 = Project(userId = newId, name = "p2")
      val p3 = Project(userId = newId, name = "p3")

      await(db.projects.all) must haveSize(0)
      await(db.projects.insert(p1))
      await(db.projects.all) must haveSize(1)
      await(db.projects.all)(0) === p1
      await(db.projects.insert(p2))
      await(db.projects.insert(p3))
      await(db.projects.all) must haveSize(3)
      await(db.projects.all).sortBy(u => u.name) === Seq(p1, p2, p3)
      await(db.projects.find(p2.id.get)) === Some(p2)
      await(db.projects.delete(p2))
      await(db.projects.all) must haveSize(2)
      await(db.projects.find(p2.id.get)) must beNone
      await(db.projects.update(p3.copy(name = "p3-new")))
      await(db.projects.find(p3.id.get)).get.name === "p3-new"

      ok
    }
  }
}
