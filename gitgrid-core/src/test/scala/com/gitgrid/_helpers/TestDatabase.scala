package com.gitgrid

import java.util.UUID

import com.gitgrid.models.Database
import reactivemongo.api._

import scala.concurrent.ExecutionContext

class TestDatabase(val mongoDbDatabase: DefaultDB, val collectionNamePrefix: String = "")(implicit ec: ExecutionContext) extends Database(mongoDbDatabase, collectionNamePrefix)

object TestDatabase {
  private var connections = Map.empty[(Seq[String]), MongoConnection]

  private def connection(nodes: Seq[String]) = synchronized {
    if (connections.contains(nodes)) connections(nodes)
    else {
      val connection = Database.mongoDbDriver.connection(nodes)
      connections = connections ++ Map(nodes -> connection)
      connection
    }
  }

  def create(coreConf: CoreConfig)(implicit ec: ExecutionContext): TestDatabase = {
    val conn = connection(coreConf.mongoDbServers)
    val data = conn(coreConf.mongoDbDatabaseName)
    val prefix = s"${UUID.randomUUID().toString.replace("-", "")}_"

    new TestDatabase(data, prefix)
  }
}
