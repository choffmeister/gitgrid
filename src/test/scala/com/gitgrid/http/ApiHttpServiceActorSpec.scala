package com.gitgrid.http

import org.specs2.mutable._
import spray.testkit._
import com.gitgrid._
import akka.testkit.TestActorRef
import spray.http.StatusCodes._
import com.gitgrid.models._
import reactivemongo.bson._
import scala.concurrent.ExecutionContext

class ApiHttpServiceActorSpec extends Specification with Specs2RouteTest with AsyncUtils {
  import JsonProtocol._
  override implicit val executor = ExecutionContext.Implicits.global
  def newId = BSONObjectID.generate

  "ApiHttpServiceActorSpec" should {
    "respond to ping requests" in new TestApiHttpService {
      Post("/api/ping") ~> route ~> check {
        status === OK
        responseAs[String] === "pong"
      }
    }
  }

  "ApiHttpServiceActorSpec /auth" should {
    "handle login requests" in new TestApiHttpService {
      val db = Database()
      val u1 = await(db.users.insert(User(id = Some(newId), userName = "user1")))
      val p1 = await(db.userPasswords.insert(UserPassword(id = Some(newId), userId = u1.id.get, createdAt = BSONDateTime(System.currentTimeMillis), hash = "pass1", hashAlgorithm = "plain")))
      val u2 = await(db.users.insert(User(id = Some(newId), userName = "user2")))
      val p2 = await(db.userPasswords.insert(UserPassword(id = Some(newId), userId = u2.id.get, createdAt = BSONDateTime(System.currentTimeMillis), hash = "pass2", hashAlgorithm = "plain")))

      Post("/api/auth/login", AuthenticationRequest("user1", "pass1")) ~> sealedRoute ~> check {
        status === OK
        responseAs[AuthenticationResponse].user === Some(u1)
      }

      Post("/api/auth/login", AuthenticationRequest("user2", "pass2")) ~> sealedRoute ~> check {
        status === OK
        responseAs[AuthenticationResponse].user === Some(u2)
      }

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

    "handle logout requests" in new TestApiHttpService {
      Post("/api/auth/logout") ~> route ~> check {
        status === OK
        responseAs[String] === "Logout"
      }
    }
  }
}

trait TestApiHttpService extends TestActorSystem with TestConfig {
  val apiHttpServiceRef = TestActorRef(new ApiHttpServiceActor)
  val apiHttpService = apiHttpServiceRef.underlyingActor

  def route = apiHttpService.route
  def sealedRoute = apiHttpService.sealRoute(route)
}
