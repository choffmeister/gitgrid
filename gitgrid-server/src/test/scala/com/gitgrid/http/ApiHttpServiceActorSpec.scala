package com.gitgrid.http

import java.util.Date

import akka.testkit._
import com.gitgrid._
import com.gitgrid.auth._
import com.gitgrid.git._
import com.gitgrid.http.routes._
import com.gitgrid.models.{Project, User}
import org.specs2.mutable._
import reactivemongo.bson.BSONObjectID
import spray.http.HttpHeaders._
import spray.http.StatusCodes._
import spray.http._
import spray.testkit._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, duration}

class ApiHttpServiceActorSpec extends Specification with Specs2RouteTest with AsyncUtils with JsonProtocol {
  override implicit val executor = ExecutionContext.Implicits.global
  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration(5000, duration.MILLISECONDS))

  "ApiHttpServiceActor misc routes" should {
    "GET /ping respond to ping requests" in new TestApiHttpService {
      Post("/api/ping") ~> route ~> check {
        status === OK
        responseAs[String] === "pong"
      }
    }
  }

  "ApiHttpServiceActor authentication routes" should {
    "POST /auth/token/create accept authentication request with valid credentials" in new TestApiHttpService {
      Get("/api/auth/token/create") ~> auth("user1", "pass1") ~> sealedRoute ~> check {
        status === OK
        val res = responseAs[OAuth2AccessTokenResponse]
        JsonWebToken.read(res.accessToken).get._2.subject === user1.id.stringify
      }

      Get("/api/auth/token/create") ~> auth("user2", "pass2") ~> sealedRoute ~> check {
        status === OK
        val res = responseAs[OAuth2AccessTokenResponse]
        JsonWebToken.read(res.accessToken).get._2.subject === user2.id.stringify
      }
    }

    "POST /auth/token/create reject authentication request with invalid credentials" in new TestApiHttpService {
      Get("/api/auth/token/create") ~> auth("user1", "pass2") ~> sealedRoute ~> check {
        status === Unauthorized
      }

      Get("/api/auth/token/create") ~> auth("user2", "pass1") ~> sealedRoute ~> check {
        status === Unauthorized
      }

      Get("/api/auth/token/create") ~> auth("user", "pass") ~> sealedRoute ~> check {
        status === Unauthorized
      }
    }
  }

  "ApiHttpServiceActor user routes" should {
    "GET /users list users" in new TestApiHttpService {
      Get("/api/users") ~> route ~> check { responseAs[List[User]] === List(user1, user2) }
    }

    "GET /users/{userName} return specific user" in new TestApiHttpService {
      Get("/api/users/user1") ~> route ~> check { responseAs[User] === user1 }
      Get("/api/users/user0") ~> sealedRoute ~> check { status === NotFound }
    }

    "POST /users/register create a new user account" in new TestApiHttpService {
      await(db.users.all) must haveSize(2)
      Get("/api/auth/state") ~> auth("user3", "pass3") ~> sealedRoute ~> check { status === Unauthorized }

      Post("/api/users/register", RegistrationRequest("user3", "a3@b3.cd", "pass3")) ~> route ~> check {
        status === OK
        val res = responseAs[User]
        res.id !== BSONObjectID("00" * 12)
        res.userName === "user3"
        res.createdAt.value must beGreaterThan(0L)
        res.updatedAt.value must beGreaterThan(0L)
      }

      await(db.users.all) must haveSize(3)
      Get("/api/auth/state") ~> auth("user3", "pass3") ~> route ~> check { responseAs[User].userName === "user3" }
    }

    "POST /users/register fail on duplicate user name" in new TestApiHttpService {
      Post("/api/users/register", RegistrationRequest("user1","a1@b1.cd", "pass1")) ~> sealedRoute ~> check {
        status === InternalServerError
      }
    }
  }

  "ApiHttpServiceActor project routes" should {
    "POST /projects create a new project" in new TestApiHttpService {
      val newProject = Project(ownerId = user1.id, name = "project-new")
      Get("/api/projects/user1/project-new") ~> auth("user1", "pass1") ~> sealedRoute ~> check { status === NotFound }
      Post("/api/projects", newProject) ~> auth("user1", "pass1") ~> route ~> check {
        status === OK
        val res = responseAs[Project]
        res.id !== BSONObjectID("00" * 12)
        res.ownerId === user1.id
        res.ownerName === user1.userName
        res.createdAt.value must beGreaterThan(0L)
        res.updatedAt.value must beGreaterThan(0L)
      }
      Get("/api/projects/user1/project-new") ~> auth("user1", "pass1") ~> route ~> check { status === OK }
    }

    "POST /projects fail on unsufficient authentication" in new TestApiHttpService {
      val newProject = Project(ownerId = user1.id, name = "project-new")
      Post("/api/projects", newProject) ~> sealedRoute ~> check { status === Unauthorized }
    }

    "POST /projects fail on creating a project for another user" in new TestApiHttpService {
      val newProject = Project(ownerId = user1.id, name = "project-new")
      Post("/api/projects", newProject) ~> auth("user2", "pass2") ~> sealedRoute ~> check { status === Forbidden }
    }

    "GET /projects list projects" in new TestApiHttpService {
      Get("/api/projects") ~> route ~> check { responseAs[List[Project]] === List(project3) }
      Get("/api/projects") ~> auth("user1", "pass1") ~> route ~> check { responseAs[List[Project]] === List(project1, project3) }
      Get("/api/projects") ~> auth("user2", "pass2") ~> route ~> check { responseAs[List[Project]] === List(project2, project3) }
    }

    "GET /projects/{userName} list projects for user" in new TestApiHttpService {
      Get("/api/projects/user1") ~> route ~> check { responseAs[List[Project]] === List(project3) }
      Get("/api/projects/user1") ~> auth("user1", "pass1") ~> route ~> check { responseAs[List[Project]] === List(project1, project3) }
      Get("/api/projects/user1") ~> auth("user2", "pass2") ~> route ~> check { responseAs[List[Project]] === List(project3) }
      Get("/api/projects/user2") ~> route ~> check { responseAs[List[Project]] === Nil }
      Get("/api/projects/user2") ~> auth("user1", "pass1") ~> route ~> check { responseAs[List[Project]] === Nil }
      Get("/api/projects/user2") ~> auth("user2", "pass2") ~> route ~> check { responseAs[List[Project]] === List(project2) }
      Get("/api/projects/user3") ~> sealedRoute ~> check { status === NotFound }
    }

    "GET /projects/{userName}/{projectName} return specific project" in new TestApiHttpService {
      Get("/api/projects/user1/project1") ~> auth("user1", "pass1") ~> route ~> check { responseAs[Project] === project1 }
      Get("/api/projects/user2/project2") ~> auth("user2", "pass2") ~> route ~> check { responseAs[Project] === project2 }
      Get("/api/projects/user1/project3") ~> auth("user1", "pass1") ~> route ~> check { responseAs[Project] === project3 }
      Get("/api/projects/user1/project3") ~> auth("user2", "pass2") ~> route ~> check { responseAs[Project] === project3 }
      Get("/api/projects/user1/project3") ~> route ~> check { responseAs[Project] === project3 }
    }

    "GET /projects/{userName}/{projectName} deny access to ungranted projects with correct HTTP return code" in new TestApiHttpService {
      Get("/api/projects/user0/project0") ~> sealedRoute ~> check { status === NotFound }

      Get("/api/projects/user1/project0") ~> sealedRoute ~> check { status === Unauthorized }
      Get("/api/projects/user1/project1") ~> sealedRoute ~> check { status === Unauthorized }
      Get("/api/projects/user1/project3") ~> sealedRoute ~> check { status === OK }
      Get("/api/projects/user2/project0") ~> sealedRoute ~> check { status === Unauthorized }
      Get("/api/projects/user2/project2") ~> sealedRoute ~> check { status === Unauthorized }

      Get("/api/projects/user1/project0") ~> auth("user1", "pass1") ~> sealedRoute ~> check { status === NotFound }
      Get("/api/projects/user1/project1") ~> auth("user1", "pass1") ~> sealedRoute ~> check { status === OK }
      Get("/api/projects/user1/project3") ~> auth("user1", "pass1") ~> sealedRoute ~> check { status === OK }
      Get("/api/projects/user2/project0") ~> auth("user1", "pass1") ~> sealedRoute ~> check { status === Forbidden }
      Get("/api/projects/user2/project2") ~> auth("user1", "pass1") ~> sealedRoute ~> check { status === Forbidden }

      Get("/api/projects/user1/project0") ~> auth("user2", "pass2") ~> sealedRoute ~> check { status === Forbidden }
      Get("/api/projects/user1/project1") ~> auth("user2", "pass2") ~> sealedRoute ~> check { status === Forbidden }
      Get("/api/projects/user1/project3") ~> auth("user2", "pass2") ~> sealedRoute ~> check { status === OK }
      Get("/api/projects/user2/project0") ~> auth("user2", "pass2") ~> sealedRoute ~> check { status === NotFound }
      Get("/api/projects/user2/project2") ~> auth("user2", "pass2") ~> sealedRoute ~> check { status === OK }
    }
  }

  "ApiHttpServiceActor GIT routes" should {
    "GET /projects/{userName}/{projectName}/git/branches list branches" in new TestApiHttpService {
      Get("/api/projects/user1/project1/git/branches") ~> auth("user1", "pass1") ~> route ~> check {
        status === OK
        val response = responseAs[List[GitRef]]
        response must beEmpty
      }

      Get("/api/projects/user2/project2/git/branches") ~> auth("user2", "pass2") ~> route ~> check {
        status === OK
        val response = responseAs[List[GitRef]]
        response must contain(GitRef("refs/heads/master", "bf3c1e0ca32e74080b6378506827b9cbc28bbffb"))
      }
    }

    "GET /projects/{userName}/{projectName}/git/tags list tags" in new TestApiHttpService {
      Get("/api/projects/user1/project1/git/tags") ~> auth("user1", "pass1") ~> route ~> check {
        status === OK
        val response = responseAs[List[GitRef]]
        response must beEmpty
      }

      Get("/api/projects/user2/project2/git/tags") ~> auth("user2", "pass2") ~> route ~> check {
        status === OK
        val response = responseAs[List[GitRef]]
        response must contain(GitRef("refs/tags/v0.0", "04ebfd7c1c3ac45cfb204386cff91a21819202b1"))
        response must contain(GitRef("refs/tags/v0.1", "7902cad7ce1e0cf079540c64080816b3e695fdb9"))
      }
    }

    "GET /projects/{userName}/{projectName}/git/commits list commits" in new TestApiHttpService {
      Get("/api/projects/user1/project1/git/commits") ~> auth("user1", "pass1") ~> route ~> check {
        status === OK
        val response = responseAs[List[GitCommit]]
        response === Nil
      }

      Get("/api/projects/user2/project2/git/commits") ~> auth("user2", "pass2") ~> route ~> check {
        status === OK
        val response = responseAs[List[GitCommit]]
      }
    }

    "GET /projects/{userName}/{projectName}/git/commits/{ref} list commits" in new TestApiHttpService {
      Get("/api/projects/user1/project1/git/commits/master") ~> auth("user1", "pass1") ~> sealedRoute ~> check {
        status === NotFound
      }

      Get("/api/projects/user2/project2/git/commits/master") ~> auth("user2", "pass2") ~> route ~> check {
        status === OK
        val response = responseAs[List[GitCommit]]
      }
    }

    "GET /projects/{userName}/{projectName}/git/commit/{id} return specific commit" in new TestApiHttpService {
      Get("/api/projects/user1/project1/git/commit/0000000000000000000000000000000000000000") ~> auth("user1", "pass1") ~> sealedRoute ~> check {
        status === NotFound
      }

      Get("/api/projects/user2/project2/git/commit/bf3c1e0ca32e74080b6378506827b9cbc28bbffb") ~> auth("user2", "pass2") ~> route ~> check {
        status === OK
        val response = responseAs[GitCommit]
      }
    }

    "GET /projects/{userName}/{projectName}/git/tree/{id} return specific tree" in new TestApiHttpService {
      Get("/api/projects/user1/project1/git/tree/0000000000000000000000000000000000000000") ~> auth("user1", "pass1") ~> sealedRoute ~> check {
        status === NotFound
      }

      Get("/api/projects/user2/project2/git/tree/aae19ad8d143bbe2f70858e8cd641847822c9080") ~> auth("user2", "pass2") ~> route ~> check {
        status === OK
        val response = responseAs[GitTree]
      }
    }

    "GET /projects/{userName}/{projectName}/git/blob/{id} return specific blob" in new TestApiHttpService {
      Get("/api/projects/user1/project1/git/blob/0000000000000000000000000000000000000000") ~> auth("user1", "pass1") ~> sealedRoute ~> check {
        status === NotFound
      }

      Get("/api/projects/user2/project2/git/blob/bb228175807fabf88754bf44be67fc19aaaff686") ~> auth("user2", "pass2") ~> route ~> check {
        status === OK
        val response = responseAs[GitBlob]
      }
    }

    "GET /projects/{userName}/{projectName}/git/blob-raw/{id} return specific blob content" in new TestApiHttpService {
      Get("/api/projects/user1/project1/git/blob-raw/0000000000000000000000000000000000000000") ~> auth("user1", "pass1") ~> sealedRoute ~> check {
        status === NotFound
      }

      Get("/api/projects/user2/project2/git/blob-raw/bb228175807fabf88754bf44be67fc19aaaff686") ~> auth("user2", "pass2") ~> route ~> check {
        status === OK
        responseAs[String] must contain("Version 0.1")
      }
    }

    "GET /projects/{userName}/{projectName}/git/tree/{ref}/ return specific tree" in new TestApiHttpService {
      Get("/api/projects/user2/project2/git/tree/unknownbranch/") ~> auth("user2", "pass2") ~> sealedRoute ~> check {
        status === NotFound
      }

      Get("/api/projects/user2/project2/git/tree/master/") ~> auth("user2", "pass2") ~> route ~> check {
        status === OK
        val response = responseAs[GitTree]
      }
    }

    "GET /projects/{userName}/{projectName}/git/tree/{ref}/{path} return specific tree" in new TestApiHttpService {
      Get("/api/projects/user2/project2/git/tree/master/unknownfolder") ~> auth("user2", "pass2") ~> sealedRoute ~> check {
        status === NotFound
      }

      Get("/api/projects/user2/project2/git/tree/master/src") ~> auth("user2", "pass2") ~> route ~> check {
        status === OK
        val response = responseAs[GitTree]
      }
    }

    "GET /projects/{userName}/{projectName}/git/blob/{ref}/{path} return specific blob" in new TestApiHttpService {
      Get("/api/projects/user2/project2/git/blob/master/unknownfile.txt") ~> auth("user2", "pass2") ~> sealedRoute ~> check {
        status === NotFound
      }

      Get("/api/projects/user2/project2/git/blob/master/README.md") ~> auth("user2", "pass2") ~> route ~> check {
        status === OK
        val response = responseAs[GitBlob]
      }
    }

    "GET /projects/{userName}/{projectName}/git/blob-raw/{ref}/{path} return specific blob content" in new TestApiHttpService {
      Get("/api/projects/user2/project2/git/blob-raw/master/unknownfile.txt") ~> auth("user2", "pass2") ~> sealedRoute ~> check {
        status === NotFound
      }

      Get("/api/projects/user2/project2/git/blob-raw/master/README.md") ~> auth("user2", "pass2") ~> route ~> check {
        status === OK
        responseAs[String] must contain("Version 0.1")
      }
    }
  }

  "ApiHttpServiceActor handle bearer tokens" should {
    "accept a valid bearer token" in new TestApiHttpService {
      Post("/api/auth/token/create") ~> auth("user1", "pass1") ~> route ~> check {
        val tokenStr = responseAs[OAuth2AccessTokenResponse].accessToken

        Get("/api/auth/state") ~> addHeader(`Authorization`(OAuth2BearerToken(tokenStr))) ~> route ~> check {
          status === OK
          responseAs[User].userName == "user1"
        }
      }
    }

    "reject an expired bearer token" in new TestApiHttpService {
      Post("/api/auth/token/create") ~> auth("user1", "pass1") ~> route ~> check {
        val res = responseAs[OAuth2AccessTokenResponse]
        val tokenStr = res.accessToken
        Thread.sleep(res.expiresIn * 1000 + 500)

        Get("/api/auth/state") ~> addHeader(`Authorization`(OAuth2BearerToken(tokenStr))) ~> sealedRoute ~> check {
          status === Unauthorized
          val authenticateHeader = header[`WWW-Authenticate`]
          authenticateHeader must beSome
          authenticateHeader.get.value must contain("expired")
        }
      }
    }

    "reject manipulated bearer token" in new TestApiHttpService {
      Post("/api/auth/token/create") ~> auth("user1", "pass1") ~> route ~> check {
        val res = responseAs[OAuth2AccessTokenResponse]
        val tokenStr = res.accessToken
        val (header, token, signature) = JsonWebToken.read(tokenStr).get
        val tokenStr2 = JsonWebToken.write(
          header,
          token.copy(expiresAt = new Date(token.expiresAt.getTime + 10000)),
          signature
        )

        Get("/api/auth/state") ~> addHeader(`Authorization`(OAuth2BearerToken(tokenStr2))) ~> sealedRoute ~> check { status === Unauthorized }
      }
    }

    "renew an expired bearer token" in new TestApiHttpService {
      Post("/api/auth/token/create") ~> auth("user1", "pass1") ~> route ~> check {
        val res = responseAs[OAuth2AccessTokenResponse]
        val tokenStr = res.accessToken
        Thread.sleep(res.expiresIn * 1000 + 500)

        Get("/api/auth/token/renew") ~> addHeader(`Authorization`(OAuth2BearerToken(tokenStr))) ~> route ~> check {
          val token2Str = responseAs[OAuth2AccessTokenResponse].accessToken

          Get("/api/auth/state") ~> addHeader(`Authorization`(OAuth2BearerToken(token2Str))) ~> route ~> check {
            status === OK
          }
        }
      }
    }

    "respond with correct HTTP challenge headers" in new TestApiHttpService {
      def extractAuthHeaders(headers: List[HttpHeader]) =
        headers.filter(_.isInstanceOf[`WWW-Authenticate`]).map(_.asInstanceOf[`WWW-Authenticate`])

      Get("/api/auth/state") ~> sealedRoute ~> check {
        status === Unauthorized
        val authHeaders = extractAuthHeaders(headers)
        authHeaders.count(_.challenges.count(_.scheme.toLowerCase == "bearer") > 0) === 1
        authHeaders.count(_.challenges.count(_.scheme.toLowerCase == "basic") > 0) === 1
      }

      Get("/api/auth/state") ~> addHeader("X-WWW-Authenticate-Filter", "Bearer") ~> sealedRoute ~> check {
        status === Unauthorized
        val authHeaders = extractAuthHeaders(headers)
        authHeaders.count(_.challenges.count(_.scheme.toLowerCase == "bearer") > 0) === 1
        authHeaders.count(_.challenges.count(_.scheme.toLowerCase == "basic") > 0) === 0
      }

      Get("/api/projects/user1/unknown-project") ~> sealedRoute ~> check {
        status === Unauthorized
        val authHeaders = extractAuthHeaders(headers)
        authHeaders.count(_.challenges.count(_.scheme.toLowerCase == "bearer") > 0) === 1
        authHeaders.count(_.challenges.count(_.scheme.toLowerCase == "basic") > 0) === 1
      }

      Get("/api/projects/user1/unknown-project") ~> addHeader("X-WWW-Authenticate-Filter", "Bearer") ~> sealedRoute ~> check {
        status === Unauthorized
        val authHeaders = extractAuthHeaders(headers)
        authHeaders.count(_.challenges.count(_.scheme.toLowerCase == "bearer") > 0) === 1
        authHeaders.count(_.challenges.count(_.scheme.toLowerCase == "basic") > 0) === 0
      }
    }
  }

  def auth(userName: String, password: String) =
    addHeader(HttpHeaders.Authorization(BasicHttpCredentials(userName, password)))
}

trait TestApiHttpService extends TestActorSystem with HttpTestEnvironment {
  val apiHttpServiceRef = TestActorRef(new ApiHttpServiceActor(coreConf, httpConf, db))
  val apiHttpService = apiHttpServiceRef.underlyingActor

  def route = apiHttpService.route
  def sealedRoute = apiHttpService.sealRoute(route)
}
