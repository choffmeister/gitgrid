package com.gitgrid

import org.specs2.specification.Scope
import java.util.UUID

trait TestConfig extends Scope {
  implicit val config = {
    val loaded = Config()
    loaded.copy(mongoDbDatabaseName = loaded.mongoDbDatabaseName + "-" + UUID.randomUUID())
  }
}
