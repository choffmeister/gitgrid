package com.gitgrid.http

import akka.testkit._
import com.gitgrid._
import com.gitgrid.http.routes._
import com.gitgrid.models.{Project, User}
import org.specs2.mutable._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{duration, ExecutionContext}
import spray.http.HttpHeaders._
import spray.http.StatusCodes._
import spray.http._
import spray.routing.authentication.UserPass
import spray.testkit._

class ApiHttpServiceActorSpec extends Specification with Specs2RouteTest with AsyncUtils with JsonProtocol {
  override implicit val executor = ExecutionContext.Implicits.global
  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration(5000, duration.MILLISECONDS))

  "ApiHttpServiceActorSpec" should {
    "respond to ping requests" in new TestApiHttpService {
      Post("/api/ping") ~> route ~> check {
        status === OK
        responseAs[String] === "pong"
      }
    }
  }

  "ApiHttpServiceActorSpec /auth" should {
    "accept authentication request with valid credentials" in new TestApiHttpService {
      Post("/api/auth/login", UserPass("user1", "pass1")) ~> sealedRoute ~> check {
        status === OK
        responseAs[AuthenticationResponse].user === Some(user1)
      }

      Post("/api/auth/login", UserPass("user2", "pass2")) ~> sealedRoute ~> check {
        status === OK
        responseAs[AuthenticationResponse].user === Some(user2)
      }
    }

    "reject authentication request with invalid credentials" in new TestApiHttpService {
      Post("/api/auth/login", UserPass("user1", "pass2")) ~> sealedRoute ~> check {
        status === OK
        responseAs[AuthenticationResponse].user must beNone
      }

      Post("/api/auth/login", UserPass("user2", "pass1")) ~> sealedRoute ~> check {
        status === OK
        responseAs[AuthenticationResponse].user must beNone
      }

      Post("/api/auth/login", UserPass("user", "pass")) ~> sealedRoute ~> check {
        status === OK
        responseAs[AuthenticationResponse].user must beNone
      }
    }

    "set and unset session cookies" in new TestApiHttpService {
      await(db.sessions.all) must haveSize(0)
      Get("/api/auth/state") ~> route ~> check {
        status === OK
        responseAs[AuthenticationResponse].user must beNone
      }

      val sessionId = Post("/api/auth/login", UserPass("user1", "pass1")) ~> route ~> check {
        val cookie = getCookie(headers)
        cookie must beSome
        cookie.get.name === "gitgrid-sid"
        cookie.get.expires must beNone
        cookie.get.content
      }

      await(db.sessions.all) must haveSize(1)
      Get("/api/auth/state") ~> addHeader(HttpHeaders.Cookie(HttpCookie("gitgrid-sid", sessionId))) ~> route ~> check {
        status === OK
        responseAs[AuthenticationResponse].user must beSome(user1)
      }

      Post("/api/auth/logout") ~> addHeader(HttpHeaders.Cookie(HttpCookie("gitgrid-sid", sessionId))) ~> route ~> check {
        val cookie = getCookie(headers)
        cookie must beSome
        cookie.get.name === "gitgrid-sid"
        cookie.get.expires must beSome
        cookie.get.expires.get.clicks must beLessThan(System.currentTimeMillis)
      }

      await(db.sessions.all) must haveSize(0)
      Get("/api/auth/state") ~> addHeader(HttpHeaders.Cookie(HttpCookie("gitgrid-sid", sessionId))) ~> route ~> check {
        status === OK
        responseAs[AuthenticationResponse].user must beNone
      }
    }

    "handle logout requests" in new TestApiHttpService {
      Post("/api/auth/logout") ~> route ~> check {
        status === OK
        responseAs[AuthenticationResponse].user must beNone
      }
    }

    "allow registration" in new TestApiHttpService {
      await(db.users.all) must haveSize(2)

      Post("/api/auth/register", UserPass("user3", "pass3")) ~> route ~> check {
        status === OK
        val res = responseAs[User]
        res.id must beSome
        res.userName === "user3"
      }

      await(db.users.all) must haveSize(3)

      Post("/api/auth/login", UserPass("user3", "pass3")) ~> route ~> check {
        val cookie = getCookie(headers)
        cookie must beSome
        cookie.get.name === "gitgrid-sid"
      }
    }
  }

  "ApiHttpServiceActorSpec /users" should {
    "yield users" in new TestApiHttpService {
      Get("/api/users/user1") ~> route ~> check { responseAs[User] === user1 }
      Get("/api/users/user0") ~> sealedRoute ~> check { status === NotFound }
    }
  }

  "ApiHttpServiceActorSpec /projects" should {
    "yield projects" in new TestApiHttpService {
      Get("/api/projects/user1/project1") ~> route ~> check { responseAs[Project] === project1 }
      Get("/api/projects/user2/project2") ~> route ~> check { responseAs[Project] === project2 }
      Get("/api/projects/user1/project2") ~> sealedRoute ~> check { status === NotFound }
      Get("/api/projects/user2/project1") ~> sealedRoute ~> check { status === NotFound }
    }
  }

  def getCookie(headers: List[HttpHeader]): Option[HttpCookie] = {
    val header = headers.find(h => h.name.toLowerCase == "set-cookie")
    header match {
      case Some(header) => Some(header.asInstanceOf[`Set-Cookie`].cookie)
      case _ => None
    }
  }
}

trait TestApiHttpService extends TestActorSystem with TestDatabase {
  val apiHttpServiceRef = TestActorRef(new ApiHttpServiceActor(db))
  val apiHttpService = apiHttpServiceRef.underlyingActor

  def route = apiHttpService.route
  def sealedRoute = apiHttpService.sealRoute(route)
}
