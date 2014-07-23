package com.gitgrid.http.routes

import com.gitgrid.Config
import com.gitgrid.models._
import scala.concurrent._

class UsersRoutes(val cfg: Config, val db: Database)(implicit val executor: ExecutionContext) extends Routes {
  def route =
    pathEnd {
      complete(db.users.all)
    } ~
    userPathPrefix { user =>
      complete(user)
    }
}
