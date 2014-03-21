package com.gitgrid.git

import akka.pattern._
import akka.testkit._
import com.gitgrid.managers.ProjectManager
import com.gitgrid.models._
import com.gitgrid._
import org.specs2.mutable._
import spray.http.HttpHeaders.Authorization
import spray.http.HttpMethods._
import spray.http.StatusCodes._
import spray.http._

class GitHttpServiceActorTest extends Specification with AsyncUtils {
  "GitHttpServiceActor" should {
    "forbid access with dump HTTP protocol" in new TestActorSystem with TestDatabase {
      val gitService = TestActorRef(new GitHttpServiceActor(db))

      val req1 = HttpRequest(method = GET, uri = Uri("/user1/project1.git/info/refs"))
      val res1 = await(gitService ? req1).asInstanceOf[HttpResponse]
      res1.status === Forbidden
    }

    "deny access with invalid credentials" in new TestActorSystem with TestDatabase {
      val gitService = TestActorRef(new GitHttpServiceActor(db))

      val req1 = HttpRequest(method = GET, uri = Uri("/user1/project1.git/info/refs?service=git-upload-pack"))
      val res1 = await(gitService ? req1).asInstanceOf[HttpResponse]
      res1.status === Unauthorized

      val req2 = authorize(HttpRequest(method = GET, uri = Uri("/user1/project1.git/info/refs?service=git-upload-pack")), "user0", "pass1")
      val res2 = await(gitService ? req2).asInstanceOf[HttpResponse]
      res2.status === Unauthorized

      val req3 = authorize(HttpRequest(method = GET, uri = Uri("/user1/project1.git/info/refs?service=git-upload-pack")), "user1", "pass2")
      val res3 = await(gitService ? req3).asInstanceOf[HttpResponse]
      res3.status === Unauthorized
    }

    "deny access with insufficient permissions" in new TestActorSystem with TestDatabase {
      val gitService = TestActorRef(new GitHttpServiceActor(db))

      val req1 = authorize(HttpRequest(method = GET, uri = Uri("/user1/project1.git/info/refs?service=git-upload-pack")), "user2", "pass2")
      val res1 = await(gitService ? req1).asInstanceOf[HttpResponse]
      res1.status === Unauthorized
    }

    "allow access with valid credentials via smart HTTP protocol" in new TestActorSystem with TestDatabase {
      val gitService = TestActorRef(new GitHttpServiceActor(db))

      val req1 = authorize(HttpRequest(method = GET, uri = Uri("/user1/project1.git/info/refs?service=git-upload-pack")), "user1", "pass1")
      val res1 = await(gitService ? req1).asInstanceOf[HttpResponse]
      res1.status === OK
      res1.entity.asString must contain("0000000000000000000000000000000000000000")
      res1.entity.asString must endWith("0000")

      val req2 = authorize(HttpRequest(method = GET, uri = Uri("/user2/project2.git/info/refs?service=git-upload-pack")), "user2", "pass2")
      val res2 = await(gitService ? req2).asInstanceOf[HttpResponse]
      res2.status === OK
      res2.entity.asString must contain("bf3c1e0ca32e74080b6378506827b9cbc28bbffb")
      res2.entity.asString must endWith("0000")
    }
  }

  def authorize(req: HttpRequest, user: String, pass: String) =
    req.copy(headers = req.headers ++ List(Authorization(BasicHttpCredentials(user, pass))))
}
