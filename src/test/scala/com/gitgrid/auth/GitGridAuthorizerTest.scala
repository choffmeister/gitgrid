package com.gitgrid.auth

import com.gitgrid.TestDatabase
import org.specs2.mutable._

class GitGridAuthorizerTest extends Specification {
  "GitGridAuthorizer" should {
    "work" in new TestDatabase {
      val auth = new GitGridAuthorizer(db)

      skipped("Inconclusive")
    }
  }
}
