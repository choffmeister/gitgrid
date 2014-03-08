package com.gitgrid.http

import akka.actor._
import spray.routing.HttpService
import com.gitgrid.Config

class ApiHttpServiceActor(implicit config: Config) extends Actor with ActorLogging with HttpService {
  implicit def actorRefFactory = context
  def receive = runRoute(route)

  lazy val route =
    pathPrefix("api") {
      pathPrefix("auth") {
        authRoute
      } ~
      path("ping") {
        post {
          complete("pong")
        }
      }
    }

  lazy val authRoute =
    path("login") {
      post {
        complete("Login")
      }
    } ~
    path("logout") {
      post {
        complete("Logout")
      }
    }
}
