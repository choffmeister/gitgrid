package com.gitgrid.http

import akka.testkit._
import com.gitgrid._
import org.specs2.mutable._
import spray.http.HttpMethods._
import spray.http.StatusCodes._

class GitHttpServiceActorSpec extends Specification with AsyncUtils with RequestUtils {
  "GitHttpServiceActor" should {
    "mark access with dump HTTP protocol as not implemented" in new TestActorSystem with TestEnvironment {
      implicit val gitService = TestActorRef(new GitHttpServiceActor(cfg, db))
      req(GET, "/user1/project1.git/info/refs").status === NotImplemented
    }

    "deny access to ungranted repository with correct HTTP return code" in new TestActorSystem with TestEnvironment {
      implicit val gitService = TestActorRef(new GitHttpServiceActor(cfg, db))

      reqGitRead ("user0", "project0").status === NotFound
      reqGitWrite("user0", "project0").status === NotFound

      reqGitRead ("user1", "project0").status === Unauthorized
      reqGitWrite("user1", "project0").status === Unauthorized
      reqGitRead ("user1", "project1").status === Unauthorized
      reqGitWrite("user1", "project1").status === Unauthorized
      reqGitRead ("user1", "project3").status === OK
      reqGitWrite("user1", "project3").status === Unauthorized
      reqGitRead ("user2", "project0").status === Unauthorized
      reqGitWrite("user2", "project0").status === Unauthorized
      reqGitRead ("user2", "project2").status === Unauthorized
      reqGitWrite("user2", "project2").status === Unauthorized

      reqGitRead ("user1", "project0", "user1", "pass1").status === NotFound
      reqGitWrite("user1", "project0", "user1", "pass1").status === NotFound
      reqGitRead ("user1", "project1", "user1", "pass1").status === OK
      reqGitWrite("user1", "project1", "user1", "pass1").status === OK
      reqGitRead ("user1", "project3", "user1", "pass1").status === OK
      reqGitWrite("user1", "project3", "user1", "pass1").status === OK
      reqGitRead ("user2", "project0", "user1", "pass1").status === Forbidden
      reqGitWrite("user2", "project0", "user1", "pass1").status === Forbidden
      reqGitRead ("user2", "project2", "user1", "pass1").status === Forbidden
      reqGitWrite("user2", "project2", "user1", "pass1").status === Forbidden

      reqGitRead ("user1", "project0", "user2", "pass2").status === Forbidden
      reqGitWrite("user1", "project0", "user2", "pass2").status === Forbidden
      reqGitRead ("user1", "project1", "user2", "pass2").status === Forbidden
      reqGitWrite("user1", "project1", "user2", "pass2").status === Forbidden
      reqGitRead ("user1", "project3", "user2", "pass2").status === OK
      reqGitWrite("user1", "project3", "user2", "pass2").status === Forbidden
      reqGitRead ("user2", "project0", "user2", "pass2").status === NotFound
      reqGitWrite("user2", "project0", "user2", "pass2").status === NotFound
      reqGitRead ("user2", "project2", "user2", "pass2").status === OK
      reqGitWrite("user2", "project2", "user2", "pass2").status === OK
    }

    "serve correct GIT repository to project" in new TestActorSystem with TestEnvironment {
      implicit val gitService = TestActorRef(new GitHttpServiceActor(cfg, db))

      val res1 = reqGitRead("user1", "project1", "user1", "pass1")
      res1.status === OK
      res1.entity.asString must contain("0000000000000000000000000000000000000000")
      res1.entity.asString must endWith("0000")

      val res2 = reqGitRead("user2", "project2", "user2", "pass2")
      res2.status === OK
      res2.entity.asString must contain("bf3c1e0ca32e74080b6378506827b9cbc28bbffb")
      res2.entity.asString must endWith("0000")

      val res3 = reqGitRead("user1", "project3")
      res3.status === OK
      res3.entity.asString must contain("0000000000000000000000000000000000000000")
      res3.entity.asString must endWith("0000")
    }
  }
}
