package com.gitgrid.http.routes

import com.gitgrid.Config
import com.gitgrid.managers.UserManager
import com.gitgrid.models._

import scala.concurrent._

case class RegistrationRequest(userName: String, email: String, password: String)

class UsersRoutes(val cfg: Config, val db: Database)(implicit val executor: ExecutionContext) extends Routes {
  val um = new UserManager(cfg, db)

  def route =
    pathEnd {
      complete(db.users.all)
    } ~
    path("register") {
      post {
        entity(as[RegistrationRequest]) { reg =>
          onSuccess(um.createUser(User(userName = reg.userName, email = reg.email), reg.password)) { user =>
            complete(user)
          }
        }
      }
    } ~
    userPathPrefix { user =>
      pathEnd {
        get {
          complete(user)
        }
      }
    }
}
