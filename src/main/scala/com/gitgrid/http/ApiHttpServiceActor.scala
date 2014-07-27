package com.gitgrid.http

import akka.actor._
import com.gitgrid.Config
import com.gitgrid.http.directives._
import com.gitgrid.http.routes._
import com.gitgrid.models._
import spray.routing._

class ApiHttpServiceActor(val cfg: Config, val db: Database) extends Actor with ActorLogging with HttpService with AuthenticationDirectives {
  implicit val actorRefFactory = context
  implicit val executor = context.dispatcher

  val authRoutes = new AuthRoutes(cfg, db)
  val usersRoutes = new UsersRoutes(cfg, db)
  val projectsRoutes = new ProjectsRoutes(cfg, db)

  def receive = runRoute(route)
  def route = pathPrefix("api") {
    filterHttpChallenges(_.scheme.toLowerCase != "basic") {
      pathPrefix("auth")(authRoutes.route) ~
      pathPrefix("users")(usersRoutes.route) ~
      pathPrefix("projects")(projectsRoutes.route)
    } ~
    path("ping")(complete("pong"))
  }
}
