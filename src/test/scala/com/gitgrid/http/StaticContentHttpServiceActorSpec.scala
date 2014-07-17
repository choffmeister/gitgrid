package com.gitgrid.http

import akka.pattern._
import akka.testkit._
import com.gitgrid._
import org.specs2.mutable._
import spray.http.HttpMethods._
import spray.http.StatusCodes._
import spray.http._

class StaticContentHttpServiceActorSpec extends Specification with AsyncUtils {
  "StaticContentHttpServiceActorSpec" should {
    "return HTTP 404 not found on unknown route" in new TestActorSystem with TestEnvironment {
      val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      val req1 = HttpRequest(method = GET, uri = Uri("/unknown/route"))
      val res1 = await(httpService ? req1).asInstanceOf[HttpResponse]
      res1.status === NotFound
    }

    "serve index.html" in new TestActorSystem with TestEnvironment {
      ZipUtils.unzip(classOf[Application].getResourceAsStream("/web.zip"), cfg.webDir.get)
      val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      val req1 = HttpRequest(method = GET, uri = Uri("/"))
      val res1 = await(httpService ? req1).asInstanceOf[HttpResponse]
      res1.status === OK

      val req2 = HttpRequest(method = GET, uri = Uri("/index.html"))
      val res2 = await(httpService ? req2).asInstanceOf[HttpResponse]
      res2.status === OK
    }

    "serve static content" in new TestActorSystem with TestEnvironment {
      ZipUtils.unzip(classOf[Application].getResourceAsStream("/web.zip"), cfg.webDir.get)
      val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      val req1 = HttpRequest(method = GET, uri = Uri("/scripts/app.js"))
      val res1 = await(httpService ? req1).asInstanceOf[HttpResponse]
      res1.status === OK
    }
  }
}
