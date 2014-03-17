package com.gitgrid

import com.gitgrid.git.GitRepository
import com.gitgrid.models._
import java.io.File
import java.util.UUID
import org.specs2.specification.Scope
import reactivemongo.api.MongoConnection
import reactivemongo.bson._
import scala.concurrent.ExecutionContext

trait TestDatabase extends Scope with AsyncUtils {
  def newId = BSONObjectID.generate

  val db = TestDatabase.create()

  val user1 = await(db.users.insert(User(userName = "user1")))
  val password1 = await(db.userPasswords.insert(UserPassword(userId = user1.id.get, createdAt = BSONDateTime(System.currentTimeMillis), hash = "pass1", hashAlgorithm = "plain")))
  val user2 = await(db.users.insert(User(userName = "user2")))
  val password2 = await(db.userPasswords.insert(UserPassword(userId = user2.id.get, createdAt = BSONDateTime(System.currentTimeMillis), hash = "pass2", hashAlgorithm = "plain")))

  val project1 = await(db.projects.insert(Project(userId = user1.id.get, name = "project1")))
  GitRepository.init(new File(Config.repositoriesDir, project1.id.get.stringify), bare = true)
  val project2 = await(db.projects.insert(Project(userId = user2.id.get, name = "project2")))
  GitRepository.init(new File(Config.repositoriesDir, project2.id.get.stringify), bare = true)
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
}
