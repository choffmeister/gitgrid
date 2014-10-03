package com.gitgrid.utils

import java.util.concurrent.TimeUnit

import com.gitgrid.utils.HexStringConverter._
import com.typesafe.config.{ConfigException, Config}

import scala.concurrent.duration.FiniteDuration

class RichConfig(val inner: Config) {
  def getOptionalString(path: String): Option[String] = try {
    Some(inner.getString(path))
  } catch {
    case e: ConfigException.Missing => None
  }

  def getFiniteDuration(path: String): FiniteDuration = {
    val unit = TimeUnit.MICROSECONDS
    FiniteDuration(inner.getDuration(path, unit), unit)
  }

  def getByteArray(path: String): Array[Byte] = {
    hex2bytes(inner.getString(path))
  }
}

object RichConfig {
  implicit def toRichConfig(inner: Config): RichConfig = new RichConfig(inner)
}
