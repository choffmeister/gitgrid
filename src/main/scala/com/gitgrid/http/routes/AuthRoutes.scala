package com.gitgrid.http.routes

import com.gitgrid.managers.UserManager
import com.gitgrid.models._
import scala.concurrent._
import spray.routing.authentication.UserPass

case class AuthenticationResponse(message: String, user: Option[User])
case class AuthenticationState(user: Option[User])

class AuthRoutes(val db: Database)(implicit val executor: ExecutionContext) extends Routes {
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
        authenticateOption(authenticator) { user =>
          complete(AuthenticationState(user))
        }
      }
    } ~
    path("register") {
      post {
        entity(as[UserPass]) { userPass =>
          onSuccess(um.createUser(userPass.user, userPass.pass)) { user => complete(user) }
        }
      }
    }
}
