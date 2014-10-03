package com.gitgrid

import java.io.File
import java.util.concurrent.TimeUnit

import com.gitgrid.utils.HexStringConverter._
import com.typesafe.config.{ConfigException, ConfigFactory, Config => RawConfig}

import scala.concurrent.duration.FiniteDuration

case class Config(
  httpInterface: String,
  httpPort: Int,
  httpAuthRealm: String,
  httpAuthBearerTokenSecret: Array[Byte],
  httpAuthBearerTokenLifetime: FiniteDuration,
  httpAuthPasswordValidationDelay: FiniteDuration,
  httpWebDir: Option[File],
  passwordsStorageDefaultAlgorithm: String,
  mongoDbServers: List[String],
  mongoDbDatabaseName: String,
  repositoriesDir: File
)

object Config {
  def load(): Config = {
    val raw = ConfigFactory.load("application").getConfig("gitgrid")

    Config(
      httpInterface = raw.getString("http.interface"),
      httpPort = raw.getInt("http.port"),
      httpAuthRealm = raw.getString("http.auth.realm"),
      httpAuthBearerTokenSecret = raw.getByteArray("http.auth.bearer-token.secret"),
      httpAuthBearerTokenLifetime = raw.getFiniteDuration("http.auth.bearer-token.lifetime"),
      httpAuthPasswordValidationDelay = raw.getFiniteDuration("http.auth.passwords-validation.delay"),
      httpWebDir = raw.getOptionalString("http.web-dir").map(new File(_)),
      passwordsStorageDefaultAlgorithm = raw.getString("passwords.storage.default-algorithm"),
      mongoDbServers = List(raw.getString("mongodb.host") + ":" + raw.getInt("mongodb.port")),
      mongoDbDatabaseName = raw.getString("mongodb.database"),
      repositoriesDir = new File(raw.getString("repositories-dir"))
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

    def getByteArray(path: String): Array[Byte] = {
      hex2bytes(underlying.getString(path))
    }
  }
}
