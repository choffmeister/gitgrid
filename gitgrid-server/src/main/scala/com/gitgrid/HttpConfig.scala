package com.gitgrid

import java.io.File

import com.gitgrid.utils.RichConfig._
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.FiniteDuration

case class HttpConfig(
  interface: String,
  port: Int,
  authRealm: String,
  authBearerTokenSecret: Array[Byte],
  authBearerTokenLifetime: FiniteDuration,
  authPasswordValidationDelay: FiniteDuration,
  webDir: Option[File]
)

object HttpConfig {
  def load(): HttpConfig = {
    val raw = ConfigFactory.load().getConfig("gitgrid.http")

    HttpConfig(
      interface = raw.getString("interface"),
      port = raw.getInt("port"),
      authRealm = raw.getString("auth.realm"),
      authBearerTokenSecret = raw.getByteArray("auth.bearer-token.secret"),
      authBearerTokenLifetime = raw.getFiniteDuration("auth.bearer-token.lifetime"),
      authPasswordValidationDelay = raw.getFiniteDuration("auth.password-validation.delay"),
      webDir = raw.getOptionalString("web-dir").map(new File(_))
    )
  }
}
