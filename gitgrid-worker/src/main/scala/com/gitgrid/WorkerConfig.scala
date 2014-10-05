package com.gitgrid

import com.typesafe.config.ConfigFactory

case class WorkerConfig(
  masterHostname: String,
  masterPort: Int,
  concurrency: Int
)

object WorkerConfig {
  def load(): WorkerConfig = {
    val raw = ConfigFactory.load().getConfig("gitgrid.worker-slave")

    WorkerConfig(
      masterHostname = raw.getString("master.hostname"),
      masterPort = raw.getInt("master.port"),
      concurrency = raw.getInt("concurrency")
    )
  }
}
