package com.gitgrid

import java.io.File
import java.util.concurrent.TimeUnit

import com.gitgrid.utils.HexStringConverter._
import com.typesafe.config.{ConfigException, ConfigFactory, Config => RawConfig}

import scala.concurrent.duration.FiniteDuration

case class CoreConfig(
  passwordsStorageDefaultAlgorithm: String,
  mongoDbServers: List[String],
  mongoDbDatabaseName: String,
  repositoriesDir: File
)

object CoreConfig {
  def load(): CoreConfig = {
    val raw = ConfigFactory.load().getConfig("gitgrid")

    CoreConfig(
      passwordsStorageDefaultAlgorithm = raw.getString("passwords.storage.default-algorithm"),
      mongoDbServers = List(raw.getString("mongodb.host") + ":" + raw.getInt("mongodb.port")),
      mongoDbDatabaseName = raw.getString("mongodb.database"),
      repositoriesDir = new File(raw.getString("repositories-dir"))
    )
  }
}
