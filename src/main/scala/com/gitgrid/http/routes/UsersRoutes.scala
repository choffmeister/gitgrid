package com.gitgrid.http.routes

import com.gitgrid.models._
import scala.concurrent._

class UsersRoutes(val db: Database)(implicit val executor: ExecutionContext) extends Routes {
  def route =
    userPathPrefix { user =>
      complete(user)
    }
}
