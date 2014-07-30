package com.gitgrid

import java.io.File
import java.util.concurrent.TimeUnit

import com.typesafe.config.{ConfigException, ConfigFactory, Config => RawConfig}

import scala.concurrent.duration.FiniteDuration

case class Config(
  httpInterface: String,
  httpPort: Int,
  httpLoginDelay: FiniteDuration,
  httpAuthRealm: String,
  httpAuthBearerTokenServerSecret: Seq[Byte],
  httpAuthBearerTokenMaximalLifetime: FiniteDuration,
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
      httpLoginDelay = raw.getFiniteDuration("gitgrid.http.login.delay"),
      httpAuthRealm = raw.getString("gitgrid.http.auth.realm"),
      httpAuthBearerTokenServerSecret = raw.getString("gitgrid.http.auth.bearerToken.serverSecret").getBytes("UTF-8").toSeq,
      httpAuthBearerTokenMaximalLifetime = raw.getFiniteDuration("gitgrid.http.auth.bearerToken.maximalLifetime"),
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
