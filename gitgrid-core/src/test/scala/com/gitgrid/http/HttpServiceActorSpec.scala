package com.gitgrid.http

import akka.testkit._
import com.gitgrid._
import org.specs2.mutable._
import spray.http.HttpMethods._
import spray.http.StatusCodes._

class HttpServiceActorSpec extends Specification with AsyncUtils with RequestUtils {
  "HttpServiceActor" should {
    "return HTTP 400 Bad Request on non GET requests to non API- or GIT-route" in new TestActorSystem with TestEnvironment {
      implicit val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      req(POST, "/index.html").status === BadRequest
    }
  }
}
