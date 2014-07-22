package com.gitgrid.http

import akka.pattern._
import akka.testkit._
import com.gitgrid._
import org.specs2.mutable._
import spray.http.HttpMethods._
import spray.http.StatusCodes._
import spray.http._

class HttpServiceActorSpec extends Specification with AsyncUtils with RequestUtils {
  "HttpServiceActor" should {
    "return HTTP 405 method not allowed on non GET requests to non API- or GIT-route" in new TestActorSystem with TestEnvironment {
      implicit val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      req(POST, "/index.html").status === MethodNotAllowed
    }
  }
}
