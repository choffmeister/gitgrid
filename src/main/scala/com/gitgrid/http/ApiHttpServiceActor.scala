package com.gitgrid.http

import akka.actor._
import com.gitgrid.Config
import com.gitgrid.http.directives._
import com.gitgrid.http.routes._
import com.gitgrid.models._
import spray.http.HttpHeaders._
import spray.http.OAuth2BearerToken
import spray.routing._
import spray.util._

class ApiHttpServiceActor(val cfg: Config, val db: Database) extends Actor with ActorLogging with HttpService with AuthenticationDirectives {
  implicit val actorRefFactory = context
  implicit val executor = context.dispatcher

  val authRoutes = new AuthRoutes(cfg, db)
  val usersRoutes = new UsersRoutes(cfg, db)
  val projectsRoutes = new ProjectsRoutes(cfg, db)

  def receive = runRoute(route)
  def route = pathPrefix("api") {
    filterBasicHttpChallenges {
      pathPrefix("auth")(authRoutes.route) ~
      pathPrefix("users")(usersRoutes.route) ~
      pathPrefix("projects")(projectsRoutes.route)
    } ~
    path("ping")(complete("pong"))
  }

  def filterBasicHttpChallenges: Directive0 = extract(ctx => ctx.request.headers).flatMap { headers =>
    headers.findByType[`Authorization`] match {
      case Some(`Authorization`(OAuth2BearerToken(c))) =>
        filterHttpChallenges(_.scheme.toLowerCase != "basic")
      case _ =>
        pass
    }
  }
}
