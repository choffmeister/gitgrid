package com.gitgrid

import com.gitgrid.models._
import java.util.UUID
import reactivemongo.api.MongoConnection
import reactivemongo.bson._
import scala.concurrent.ExecutionContext

trait TestDatabase extends TestConfig with AsyncUtils {
  def newId = BSONObjectID.generate

  val db = TestDatabase.create(config)

  val user1 = await(db.users.insert(User(userName = "user1")))
  val password1 = await(db.userPasswords.insert(UserPassword(userId = user1.id.get, createdAt = BSONDateTime(System.currentTimeMillis), hash = "pass1", hashAlgorithm = "plain")))
  val user2 = await(db.users.insert(User(userName = "user2")))
  val password2 = await(db.userPasswords.insert(UserPassword(userId = user2.id.get, createdAt = BSONDateTime(System.currentTimeMillis), hash = "pass2", hashAlgorithm = "plain")))
}

object TestDatabase {
  private var connections = Map.empty[(Seq[String]), MongoConnection]

  def create(config: Config)(implicit ec: ExecutionContext): Database =
    new Database(connection(config.mongoDbServers), config.mongoDbDatabaseName, s"_test_${UUID.randomUUID()}_")

  private def connection(nodes: Seq[String]) = synchronized {
    if (connections.contains(nodes)) connections(nodes)
    else {
      val connection = Database.driver.connection(nodes)
      connections = connections ++ Map(nodes -> connection)
      connection
    }
  }
}
