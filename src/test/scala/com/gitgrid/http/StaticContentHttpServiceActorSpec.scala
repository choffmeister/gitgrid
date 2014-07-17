package com.gitgrid.http

import akka.pattern._
import akka.testkit._
import com.gitgrid._
import org.specs2.mutable._
import spray.http.HttpMethods._
import spray.http.StatusCodes._
import spray.http._

class StaticContentHttpServiceActorSpec extends Specification with AsyncUtils {
  "StaticContentHttpServiceActor" should {
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
      res1.entity.asString must contain("<title>GitGrid</title>")

      val req2 = HttpRequest(method = GET, uri = Uri("/index.html"))
      val res2 = await(httpService ? req2).asInstanceOf[HttpResponse]
      res2.status === OK
      res2.entity.asString must contain("<title>GitGrid</title>")
    }

    "server index.html for GET requests to non-existent files" in new TestActorSystem with TestEnvironment {
      ZipUtils.unzip(classOf[Application].getResourceAsStream("/web.zip"), cfg.webDir.get)
      val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      val req1 = HttpRequest(method = GET, uri = Uri("/login"))
      val res1 = await(httpService ? req1).asInstanceOf[HttpResponse]
      res1.status === OK
      res1.entity.asString must contain("<title>GitGrid</title>")

      val req2 = HttpRequest(method = GET, uri = Uri("/logout"))
      val res2 = await(httpService ? req2).asInstanceOf[HttpResponse]
      res2.status === OK
      res2.entity.asString must contain("<title>GitGrid</title>")
    }

    "serve static content" in new TestActorSystem with TestEnvironment {
      ZipUtils.unzip(classOf[Application].getResourceAsStream("/web.zip"), cfg.webDir.get)
      val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      val req1 = HttpRequest(method = GET, uri = Uri("/scripts/app.js"))
      val res1 = await(httpService ? req1).asInstanceOf[HttpResponse]
      res1.status === OK
      res1.entity.asString must contain("console.log('app')");
    }
  }
}
