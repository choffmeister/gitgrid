package com.gitgrid

import java.io.File

import com.typesafe.config.{ConfigFactory, Config => RawConfig}

case class CoreConfig(
  passwordsStorageDefaultAlgorithm: String,
  mongoDbServers: List[String],
  mongoDbDatabaseName: String,
  repositoriesDir: File
)

object CoreConfig {
  lazy val raw = ConfigFactory.load().getConfig("gitgrid")

  def load(): CoreConfig = CoreConfig(
    passwordsStorageDefaultAlgorithm = raw.getString("passwords.storage.default-algorithm"),
    mongoDbServers = List(raw.getString("mongodb.host") + ":" + raw.getInt("mongodb.port")),
    mongoDbDatabaseName = raw.getString("mongodb.database"),
    repositoriesDir = new File(raw.getString("repositories-dir"))
  )
}
