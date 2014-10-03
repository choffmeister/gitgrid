package com.gitgrid.http

import akka.actor._
import com.gitgrid._
import com.gitgrid.http.directives._
import com.gitgrid.http.routes._
import com.gitgrid.models._
import spray.routing._

class ApiHttpServiceActor(val coreConf: CoreConfig, val httpConf: HttpConfig, val db: Database) extends Actor with ActorLogging with HttpService with AuthenticationDirectives {
  implicit val actorRefFactory = context
  implicit val executor = context.dispatcher

  val authRoutes = new AuthRoutes(coreConf, httpConf, db)
  val usersRoutes = new UsersRoutes(coreConf, httpConf, db)
  val projectsRoutes = new ProjectsRoutes(coreConf, httpConf, db)
  val workersRoutes = new WorkersRoutes(coreConf, httpConf, db, context)

  def receive = runRoute(route)
  def route = pathPrefix("api") {
    filterHttpChallengesByExtensionHeader {
      pathPrefix("auth")(authRoutes.route) ~
      pathPrefix("users")(usersRoutes.route) ~
      pathPrefix("projects")(projectsRoutes.route) ~
      path("workers")(workersRoutes.route)
    } ~
    path("ping")(complete("pong"))
  }
}
