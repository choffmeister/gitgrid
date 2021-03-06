package com.gitgrid

import com.gitgrid.managers._
import com.gitgrid.models._
import org.specs2.specification.Scope

trait EmptyTestEnvironment extends Scope with AsyncUtils {
  val coreConf = CoreConfig.load()
  val db = TestDatabase.create(coreConf)
  val um = new UserManager(coreConf, db)
  val pm = new ProjectManager(coreConf, db)
}

trait TestEnvironment extends EmptyTestEnvironment {
  val user1 = await(um.createUser(User(userName = "user1", email = "a1@b1.cd"), "pass1"))
  val user2 = await(um.createUser(User(userName = "user2", email = "a2@b2.cd"), "pass2"))
  val project1 = await(pm.createProject(Project(ownerId = user1.id, name = "project1", public = false)))
  val project2 = await(pm.createProject(Project(ownerId = user2.id, name = "project2", public = false)))
  val project3 = await(pm.createProject(Project(ownerId = user1.id, name = "project3", public = true)))
  TestEnvironment.unzipRepository(pm, project2, "/repo1.zip")
}

object TestEnvironment {
  def unzipRepository(projectManager: ProjectManager, project: Project, resourceName: String): Unit = {
    val dir = projectManager.getRepositoryDirectory(project.id)
    if (dir.exists) dir.delete()
    ZipUtils.unzip(getClass.getResourceAsStream(resourceName), dir)
  }
}
