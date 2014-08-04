package com.gitgrid

import java.io.File
import java.util.concurrent.TimeUnit

import com.typesafe.config.{ConfigException, ConfigFactory, Config => RawConfig}

import scala.concurrent.duration.FiniteDuration

case class Config(
  httpInterface: String,
  httpPort: Int,
  httpAuthRealm: String,
  httpAuthBearerTokenSecret: Array[Byte],
  httpAuthBearerTokenLifetime: FiniteDuration,
  passwordsValidationDelay: FiniteDuration,
  passwordsStorageDefaultAlgorithm: String,
  mongoDbServers: List[String],
  mongoDbDatabaseName: String,
  repositoriesDir: File,
  webDir: Option[File]
)

object Config {
  def load(): Config = {
    val raw = ConfigFactory.load("application").getConfig("gitgrid")

    Config(
      httpInterface = raw.getString("http.interface"),
      httpPort = raw.getInt("http.port"),
      httpAuthRealm = raw.getString("http.auth.realm"),
      httpAuthBearerTokenSecret = raw.getString("http.auth.bearer-token.secret").getBytes("UTF-8"),
      httpAuthBearerTokenLifetime = raw.getFiniteDuration("http.auth.bearer-token.lifetime"),
      passwordsValidationDelay = raw.getFiniteDuration("passwords.validation.delay"),
      passwordsStorageDefaultAlgorithm = raw.getString("passwords.storage.default-algorithm"),
      mongoDbServers = List(raw.getString("mongodb.host") + ":" + raw.getInt("mongodb.port")),
      mongoDbDatabaseName = raw.getString("mongodb.database"),
      repositoriesDir = new File(raw.getString("repositories-dir")),
      webDir = raw.getOptionalString("web-dir").map(new File(_))
    )
  }

  implicit class RichConfig(val underlying: RawConfig) extends AnyVal {
    def getOptionalString(path: String): Option[String] = try {
       Some(underlying.getString(path))
    } catch {
       case e: ConfigException.Missing => None
    }

    def getFiniteDuration(path: String): FiniteDuration = {
      val unit = TimeUnit.MICROSECONDS
      FiniteDuration(underlying.getDuration(path, unit), unit)
    }
  }
}
