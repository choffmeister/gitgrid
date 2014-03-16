package com.gitgrid

import com.typesafe.config.ConfigFactory
import java.io.File

object Config {
  lazy val raw = ConfigFactory.load("application")

  lazy val httpInterface = raw.getString("gitgrid.http.interface")
  lazy val httpPort = raw.getInt("gitgrid.http.port")
  lazy val repositoriesDir = new File(raw.getString("gitgrid.repositoriesDir"))
  lazy val mongoDbServers = List(raw.getString("gitgrid.mongodb.host") + ":" + raw.getInt("gitgrid.mongodb.port"))
  lazy val mongoDbDatabaseName = raw.getString("gitgrid.mongodb.database")
}
