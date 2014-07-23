package com.gitgrid

import com.typesafe.config.{ConfigFactory, ConfigException, Config => RawConfig}
import java.io.File

case class Config(
  httpInterface: String,
  httpPort: Int,
  mongoDbServers: List[String],
  mongoDbDatabaseName: String,
  repositoriesDir: File,
  webDir: Option[File]
)

object Config {
  def load(): Config = {
    val raw = ConfigFactory.load("application")

    Config(
      httpInterface = raw.getString("gitgrid.http.interface"),
      httpPort = raw.getInt("gitgrid.http.port"),
      mongoDbServers = List(raw.getString("gitgrid.mongodb.host") + ":" + raw.getInt("gitgrid.mongodb.port")),
      mongoDbDatabaseName = raw.getString("gitgrid.mongodb.database"),
      repositoriesDir = new File(raw.getString("gitgrid.repositoriesDir")),
      webDir = raw.getOptionalString("gitgrid.webDir").map(new File(_))
    )
  }

  implicit class RichConfig(val underlying: RawConfig) extends AnyVal {
    def getOptionalString(path: String): Option[String] = try {
       Some(underlying.getString(path))
    } catch {
       case e: ConfigException.Missing => None
    }
  }
}
