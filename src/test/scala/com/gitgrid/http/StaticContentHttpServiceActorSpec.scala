package com.gitgrid.http

import akka.actor._
import akka.pattern._
import akka.testkit._
import akka.util.Timeout
import com.gitgrid._
import org.specs2.mutable._
import spray.http.HttpMethods._
import spray.http.StatusCodes._
import spray.http._

class StaticContentHttpServiceActorSpec extends Specification with AsyncUtils with RequestUtils {
  "StaticContentHttpServiceActor" should {
    "serve index.html" in new TestActorSystem with TestEnvironment {
      ZipUtils.unzip(classOf[Application].getResourceAsStream("/web.zip"), cfg.webDir.get)
      implicit val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      val res1 = req(GET, "/")
      res1.status === OK
      res1.entity.asString must contain("<title>GitGrid</title>")

      val res2 = req(GET, "/index.html")
      res2.status === OK
      res2.entity.asString must contain("<title>GitGrid</title>")
    }

    "server index.html for GET requests to non-existent files" in new TestActorSystem with TestEnvironment {
      ZipUtils.unzip(classOf[Application].getResourceAsStream("/web.zip"), cfg.webDir.get)
      implicit val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      val res1 = req(GET, "/login")
      res1.status === OK
      res1.entity.asString must contain("<title>GitGrid</title>")

      val res2 = req(GET, "/logout")
      res2.status === OK
      res2.entity.asString must contain("<title>GitGrid</title>")

      val res3 = req(GET, "/user1/proj1")
      res3.status === OK
      res3.entity.asString must contain("<title>GitGrid</title>")
    }

    "serve static content" in new TestActorSystem with TestEnvironment {
      ZipUtils.unzip(classOf[Application].getResourceAsStream("/web.zip"), cfg.webDir.get)
      implicit val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      val res1 = req(GET, "/app/app.js")
      res1.status === OK
      res1.entity.asString must contain("console.log('app')");

      req(GET, "/app/unknown.txt").status === NotFound
    }
  }
}
