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

class StaticContentHttpServiceActorSpec extends Specification with AsyncUtils {
  "StaticContentHttpServiceActor" should {
    "serve index.html" in new TestActorSystem with TestEnvironment {
      ZipUtils.unzip(classOf[Application].getResourceAsStream("/web.zip"), cfg.webDir.get)
      val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      val res1 = req(httpService, GET, "/")
      res1.status === OK
      res1.entity.asString must contain("<title>GitGrid</title>")

      val res2 = req(httpService, GET, "/index.html")
      res2.status === OK
      res2.entity.asString must contain("<title>GitGrid</title>")
    }

    "server index.html for GET requests to non-existent files" in new TestActorSystem with TestEnvironment {
      ZipUtils.unzip(classOf[Application].getResourceAsStream("/web.zip"), cfg.webDir.get)
      val httpService = TestActorRef(new HttpServiceActor(cfg, db))

      val res1 = req(httpService, GET, "/login")
      res1.status === OK
      res1.entity.asString must contain("<title>GitGrid</title>")

      val res2 = req(httpService, GET, "/logout")
      res2.status === OK
      res2.entity.asString must contain("<title>GitGrid</title>")

      val res3 = req(httpService, GET, "/user1/proj1")
      res3.status === OK
      res3.entity.asString must contain("<title>GitGrid</title>")
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

      val res1 = req(httpService, GET, "/scripts/app.js")
      res1.status === OK
      res1.entity.asString must contain("console.log('app')");

      req(httpService, GET, "/assets/image.png").status === NotFound
      req(httpService, GET, "/scripts/unknown.js").status === NotFound
      req(httpService, GET, "/styles/style.css").status === NotFound
      req(httpService, GET, "/views/view.html").status === NotFound
    }
  }

  def req(httpActor: ActorRef, req: HttpRequest)(implicit timeout: Timeout): HttpResponse =
    await(httpActor ? req).asInstanceOf[HttpResponse]
  def req(httpActor: ActorRef, method: HttpMethod, uri: String)(implicit timeout: Timeout): HttpResponse =
    req(httpActor, HttpRequest(method = method, uri = Uri(uri)))
}
