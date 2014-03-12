package com.gitgrid.http

import akka.testkit.TestActorRef
import com.gitgrid._
import org.specs2.mutable._
import scala.concurrent.ExecutionContext
import spray.http.HttpHeaders._
import spray.http.StatusCodes._
import spray.http._
import spray.testkit._

class ApiHttpServiceActorSpec extends Specification with Specs2RouteTest with AsyncUtils with JsonProtocol {
  override implicit val executor = ExecutionContext.Implicits.global

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
      Post("/api/auth/login", AuthenticationRequest("user1", "pass1")) ~> sealedRoute ~> check {
        status === OK
        responseAs[AuthenticationResponse].user === Some(user1)
      }

      Post("/api/auth/login", AuthenticationRequest("user2", "pass2")) ~> sealedRoute ~> check {
        status === OK
        responseAs[AuthenticationResponse].user === Some(user2)
      }
    }

    "reject authentication request with invalid credentials" in new TestApiHttpService {
      Post("/api/auth/login", AuthenticationRequest("user1", "pass2")) ~> sealedRoute ~> check {
        status === OK
        responseAs[AuthenticationResponse].user must beNone
      }

      Post("/api/auth/login", AuthenticationRequest("user2", "pass1")) ~> sealedRoute ~> check {
        status === OK
        responseAs[AuthenticationResponse].user must beNone
      }

      Post("/api/auth/login", AuthenticationRequest("user", "pass")) ~> sealedRoute ~> check {
        status === OK
        responseAs[AuthenticationResponse].user must beNone
      }
    }

    "set and unset session cookies" in new TestApiHttpService {
      def getCookie(headers: List[HttpHeader]): Option[HttpCookie] = {
        val header = headers.find(h => h.name.toLowerCase == "set-cookie")
        header match {
          case Some(header) => Some(header.asInstanceOf[`Set-Cookie`].cookie)
          case _ => None
        }
      }

      await(db.sessions.all) must haveSize(0)
      Get("/api/auth/state") ~> route ~> check {
        status === OK
        responseAs[AuthenticationState].user must beNone
      }

      val sessionId = Post("/api/auth/login", AuthenticationRequest("user1", "pass1")) ~> route ~> check {
        val cookie = getCookie(headers)
        cookie must beSome
        cookie.get.name === "gitgrid-sid"
        cookie.get.expires must beNone
        cookie.get.content
      }

      await(db.sessions.all) must haveSize(1)
      Get("/api/auth/state") ~> addHeader(HttpHeaders.Cookie(HttpCookie("gitgrid-sid", sessionId))) ~> route ~> check {
        status === OK
        responseAs[AuthenticationState].user must beSome(user1)
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
        responseAs[AuthenticationState].user must beNone
      }
    }

    "handle logout requests" in new TestApiHttpService {
      Post("/api/auth/logout") ~> route ~> check {
        status === OK
        responseAs[AuthenticationState].user must beNone
      }
    }
  }
}

trait TestApiHttpService extends TestActorSystem with TestConfig with TestDatabase {
  val apiHttpServiceRef = TestActorRef(new ApiHttpServiceActor)
  val apiHttpService = apiHttpServiceRef.underlyingActor

  def route = apiHttpService.route
  def sealedRoute = apiHttpService.sealRoute(route)
}
