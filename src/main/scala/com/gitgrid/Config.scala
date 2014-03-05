package com.gitgrid

import com.typesafe.config.ConfigFactory
import java.io.File

object Config {
  lazy val raw = ConfigFactory.load("application")

  lazy val httpInterface = raw.getString("gitgrid.http.interface")
  lazy val httpPort = raw.getInt("gitgrid.http.port")
  lazy val repositoriesDir = new File(raw.getString("gitgrid.repositoriesDir"))
  lazy val mongoDbServers = List(Config.raw.getString("gitgrid.mongodb.host") + ":" + Config.raw.getInt("gitgrid.mongodb.port"))
  lazy val mongoDbDatabaseName = Config.raw.getString("gitgrid.mongodb.database")
}
