package com.gitgrid

import java.io.File
import java.util.concurrent.TimeUnit

import com.typesafe.config.{ConfigException, ConfigFactory, Config => RawConfig}

import scala.concurrent.duration.FiniteDuration

case class Config(
  httpInterface: String,
  httpPort: Int,
  httpAuthRealm: String,
  httpAuthBearerTokenServerSecret: Seq[Byte],
  httpAuthBearerTokenMaximalLifetime: FiniteDuration,
  passwordsValidationDelay: FiniteDuration,
  passwordsStorageDefaultAlgorithm: String,
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
      httpAuthRealm = raw.getString("gitgrid.http.auth.realm"),
      httpAuthBearerTokenServerSecret = raw.getString("gitgrid.http.auth.bearerToken.serverSecret").getBytes("UTF-8").toSeq,
      httpAuthBearerTokenMaximalLifetime = raw.getFiniteDuration("gitgrid.http.auth.bearerToken.maximalLifetime"),
      passwordsValidationDelay = raw.getFiniteDuration("gitgrid.passwords.validation.delay"),
      passwordsStorageDefaultAlgorithm = raw.getString("gitgrid.passwords.storage.defaultAlgorithm"),
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

    def getFiniteDuration(path: String): FiniteDuration = {
      val unit = TimeUnit.MICROSECONDS
      FiniteDuration(underlying.getDuration(path, unit), unit)
    }
  }
}
