package com.gitgrid

import com.gitgrid.managers.{ProjectManager, UserManager}
import com.gitgrid.models._
import java.util.UUID
import org.specs2.specification.Scope
import reactivemongo.api.MongoConnection
import scala.concurrent.ExecutionContext

trait TestDatabase extends Scope with AsyncUtils {
  val db = TestDatabase.create()
  val um = new UserManager(db)
  val pm = new ProjectManager(db)

  val user1 = await(um.createUser("user1", "pass1"))
  val user2 = await(um.createUser("user2", "pass2"))
  val project1 = await(pm.createProject(user1.id.get, "project1"))
  val project2 = await(pm.createProject(user2.id.get, "project2"))
  TestDatabase.unzipRepository(pm, project2, "/repo1.zip")
}

object TestDatabase {
  private var connections = Map.empty[(Seq[String]), MongoConnection]

  def create()(implicit ec: ExecutionContext): Database =
    new Database(connection(Config.mongoDbServers), Config.mongoDbDatabaseName, s"_test_${UUID.randomUUID()}_")

  private def connection(nodes: Seq[String]) = synchronized {
    if (connections.contains(nodes)) connections(nodes)
    else {
      val connection = Database.driver.connection(nodes)
      connections = connections ++ Map(nodes -> connection)
      connection
    }
  }

  def unzipRepository(projectManager: ProjectManager, project: Project, resourceName: String): Unit = {
    val dir = projectManager.getRepositoryDirectory(project.id.get)
    if (dir.exists) dir.delete()
    ZipUtils.unzip(classOf[ResourcesAnchor].getResourceAsStream(resourceName), dir)
  }
}

class ResourcesAnchor
