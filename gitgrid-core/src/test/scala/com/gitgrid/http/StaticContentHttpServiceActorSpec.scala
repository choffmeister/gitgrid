package com.gitgrid.http

import akka.testkit._
import com.gitgrid._
import org.specs2.mutable._
import spray.http.HttpMethods._
import spray.http.StatusCodes._

class StaticContentHttpServiceActorSpec extends Specification with AsyncUtils with RequestUtils {
  "StaticContentHttpServiceActor" should {
    "serve index.html" in new TestActorSystem with TestEnvironment {
      ZipUtils.unzip(getClass.getResourceAsStream("/web.zip"), cfg.webDir.get)
      implicit val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      req(GET, "/").entity.asString must contain("<title>GitGrid</title>")
      req(GET, "/index.html").entity.asString must contain("<title>GitGrid</title>")
    }

    "server index.html for GET requests to non-existent files" in new TestActorSystem with TestEnvironment {
      ZipUtils.unzip(getClass.getResourceAsStream("/web.zip"), cfg.webDir.get)
      implicit val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      req(GET, "/login").entity.asString must contain("<title>GitGrid</title>")
      req(GET, "/logout").entity.asString must contain("<title>GitGrid</title>")
      req(GET, "/user1/proj1").entity.asString must contain("<title>GitGrid</title>")
      req(GET, "/app2/app.js").entity.asString must contain("<title>GitGrid</title>")
    }

    "serve static content" in new TestActorSystem with TestEnvironment {
      ZipUtils.unzip(getClass.getResourceAsStream("/web.zip"), cfg.webDir.get)
      implicit val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      req(GET, "/app/app.js").entity.asString must contain("console.log('app')");
      req(GET, "/app/unknown.txt").status === NotFound
    }
  }
}
