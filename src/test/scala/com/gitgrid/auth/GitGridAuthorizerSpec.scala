package com.gitgrid.auth

import com.gitgrid.TestEnvironment
import org.specs2.mutable._

class GitGridAuthorizerSpec extends Specification {
  "GitGridAuthorizer" should {
    "properly grant and deny access to projects" in new TestEnvironment {
      val auth = new GitGridAuthorizer(db)

      await(auth.authorize(None, ProjectRead(project1))) === false
      await(auth.authorize(None, ProjectRead(project2))) === false
      await(auth.authorize(Some(user2), ProjectRead(project1))) === false
      await(auth.authorize(Some(user1), ProjectRead(project2))) === false
      await(auth.authorize(Some(user1), ProjectRead(project1))) === true
      await(auth.authorize(Some(user2), ProjectRead(project2))) === true
    }

    "properly grant and deny access to project repositores" in new TestEnvironment {
      val auth = new GitGridAuthorizer(db)

      await(auth.authorize(None, ProjectRepositoryReadWrite(project1))) === false
      await(auth.authorize(None, ProjectRepositoryReadWrite(project2))) === false
      await(auth.authorize(Some(user2), ProjectRepositoryReadWrite(project1))) === false
      await(auth.authorize(Some(user1), ProjectRepositoryReadWrite(project2))) === false
      await(auth.authorize(Some(user1), ProjectRepositoryReadWrite(project1))) === true
      await(auth.authorize(Some(user2), ProjectRepositoryReadWrite(project2))) === true
    }
  }
}