package com.gitgrid

import java.util.UUID
import com.gitgrid.models.Database
import reactivemongo.api.MongoConnection
import scala.concurrent.ExecutionContext

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

  def create(cfg: Config)(implicit ec: ExecutionContext): Database = {
    val conn = connection(cfg.mongoDbServers)
    val data = conn(cfg.mongoDbDatabaseName)
    val prefix = s"_test_${UUID.randomUUID()}_"

    new Database(data, prefix)
  }
}
