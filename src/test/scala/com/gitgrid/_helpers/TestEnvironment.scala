package com.gitgrid

import org.specs2.specification.Scope
import java.util.UUID

trait TestEnvironment extends Scope {
  implicit val env = Environment(Config.mongoDbServers, s"${Config.mongoDbDatabaseName}-${UUID.randomUUID().toString}")
}
