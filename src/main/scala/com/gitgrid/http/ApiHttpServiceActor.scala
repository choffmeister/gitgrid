package com.gitgrid.http

import akka.actor._
import com.gitgrid.http.routes._
import com.gitgrid.models._
import spray.routing._

class ApiHttpServiceActor(val db: Database) extends Actor with ActorLogging with HttpService {
  implicit val actorRefFactory = context
  implicit val executor = context.dispatcher

  val authRoutes = new AuthRoutes(db)
  val usersRoutes = new UsersRoutes(db)
  val projectsRoutes = new ProjectsRoutes(db)

  def receive = runRoute(route)
  def route = pathPrefix("api") {
    pathPrefix("auth")(authRoutes.route) ~
    pathPrefix("users")(usersRoutes.route) ~
    pathPrefix("projects")(projectsRoutes.route) ~
    path("ping")(complete("pong"))
  }
}
