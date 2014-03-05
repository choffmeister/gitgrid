package com.gitgrid

import akka.pattern._
import akka.testkit._
import org.specs2.mutable._
import spray.http._
import spray.http.HttpMethods._
import spray.http.StatusCodes._

class HttpServiceActorSpec extends Specification {
  "HttpServiceActorSpec" should {
    "serve requests" in new TestActorSystem {
      val httpService = TestActorRef[HttpServiceActor]

      val req1 = HttpRequest(method = GET, uri = Uri("/"))
      val res1 = await(httpService ? req1).asInstanceOf[HttpResponse]
      res1.status === OK

      ok
    }
  }
}
