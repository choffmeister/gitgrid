package com.gitgrid.http

import org.specs2.mutable._
import spray.testkit._
import com.gitgrid._
import akka.testkit.TestActorRef
import spray.http.StatusCodes._

class ApiHttpServiceActorSpec extends Specification with Specs2RouteTest {
  "ApiHttpServiceActorSpec" should {
    "respond to ping requests" in new TestApiHttpService {
      Post("/api/ping") ~> apiHttpService.route ~> check {
        status === OK
        responseAs[String] === "pong"
      }
    }
  }

  "ApiHttpServiceActorSpec /auth" should {
    "handle login requests" in new TestApiHttpService {
      Post("/api/auth/login") ~> apiHttpService.route ~> check {
        status === OK
        responseAs[String] === "Login"
      }
    }

    "handle logout requests" in new TestApiHttpService {
      Post("/api/auth/logout") ~> apiHttpService.route ~> check {
        status === OK
        responseAs[String] === "Logout"
      }
    }
  }
}

trait TestApiHttpService extends TestActorSystem with TestConfig {
  val apiHttpServiceRef = TestActorRef(new ApiHttpServiceActor)
  val apiHttpService = apiHttpServiceRef.underlyingActor
}
