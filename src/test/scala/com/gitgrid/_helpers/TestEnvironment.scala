package com.gitgrid

import com.gitgrid.managers._
import com.gitgrid.models._
import org.specs2.specification.Scope

trait EmptyTestEnvironment extends Scope with AsyncUtils {
  val cfg = Config.load()
  val db = TestDatabase.create(cfg)
  val um = new UserManager(db)
  val pm = new ProjectManager(cfg, db)
}

trait TestEnvironment extends EmptyTestEnvironment {
  val user1 = await(um.createUser("user1", "pass1"))
  val user2 = await(um.createUser("user2", "pass2"))
  val project1 = await(pm.createProject(user1.id.get, "project1"))
  val project2 = await(pm.createProject(user2.id.get, "project2"))
  TestEnvironment.unzipRepository(pm, project2, "/repo1.zip")
}

object TestEnvironment {
  def unzipRepository(projectManager: ProjectManager, project: Project, resourceName: String): Unit = {
    val dir = projectManager.getRepositoryDirectory(project.id.get)
    if (dir.exists) dir.delete()
    ZipUtils.unzip(classOf[Application].getResourceAsStream(resourceName), dir)
  }
}