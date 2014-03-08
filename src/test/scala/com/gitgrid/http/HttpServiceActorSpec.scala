package com.gitgrid.http

import akka.pattern._
import akka.testkit._
import org.specs2.mutable._
import spray.http.HttpMethods._
import spray.http.StatusCodes._
import spray.http._
import com.gitgrid.{TestEnvironment, TestActorSystem}

class HttpServiceActorSpec extends Specification {
  "HttpServiceActorSpec" should {
    "return HTTP 404 not found on unknown route" in new TestActorSystem with TestEnvironment {
      val httpService = TestActorRef(new HttpServiceActor)

      val req1 = HttpRequest(method = GET, uri = Uri("/"))
      val res1 = await(httpService ? req1).asInstanceOf[HttpResponse]
      res1.status === NotFound

      ok
    }
  }
}
