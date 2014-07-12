package com.gitgrid

import com.typesafe.config.ConfigFactory
import java.io.File

case class Config(
  httpInterface: String,
  httpPort: Int,
  mongoDbServers: List[String],
  mongoDbDatabaseName: String,
  repositoriesDir: File
)

object Config {
  def load(): Config = {
    val raw = ConfigFactory.load("application")

    Config(
      httpInterface = raw.getString("gitgrid.http.interface"),
      httpPort = raw.getInt("gitgrid.http.port"),
      repositoriesDir = new File(raw.getString("gitgrid.repositoriesDir")),
      mongoDbServers = List(raw.getString("gitgrid.mongodb.host") + ":" + raw.getInt("gitgrid.mongodb.port")),
      mongoDbDatabaseName = raw.getString("gitgrid.mongodb.database")
    )
  }
}
