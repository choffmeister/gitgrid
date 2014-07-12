package com.gitgrid.http.routes

import com.gitgrid.Config
import com.gitgrid.managers.UserManager
import com.gitgrid.models._
import scala.concurrent._

case class AuthenticationResponse(message: String, user: Option[User])
case class RegistrationRequest(userName: String, password: String)

class AuthRoutes(val cfg: Config, val db: Database)(implicit val executor: ExecutionContext) extends Routes {
  val um = new UserManager(db)

  def route =
    path("login") {
      post {
        formsLogin(authenticator) {
          case Some(user) => complete(AuthenticationResponse("Logged in", Some(user)))
          case _ => complete(AuthenticationResponse("Invalid username or password", None))
        }
      }
    } ~
    path("logout") {
      post {
        formsLogout(authenticator) {
          complete(AuthenticationResponse("Logged out", None))
        }
      }
    } ~
    path("state") {
      get {
        authenticateOption() {
          case Some(user) => complete(AuthenticationResponse("Authenticated", Some(user)))
          case _ => complete(AuthenticationResponse("Unauthenticated", None))
        }
      }
    } ~
    path("register") {
      post {
        entity(as[RegistrationRequest]) { registration =>
          onSuccess(um.createUser(registration.userName, registration.password)) { user =>
            complete(user)
          }
        }
      }
    }
}
