package com.gitgrid

import java.util.UUID
import org.specs2.specification.Scope

trait TestConfig extends Scope {
  implicit val config = {
    val loaded = Config()
    loaded.copy(mongoDbCollectionNamePrefix = s"_test_${UUID.randomUUID()}_")
  }
}
