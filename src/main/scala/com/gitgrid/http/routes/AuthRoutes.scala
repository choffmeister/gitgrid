package com.gitgrid.http.routes

import com.gitgrid.Config
import com.gitgrid.auth._
import com.gitgrid.managers.UserManager
import com.gitgrid.models._
import spray.routing.authentication._
import scala.concurrent._

case class AuthenticationResponse(message: String, user: Option[User] = None, token: Option[String] = None)
case class RegistrationRequest(userName: String, password: String)

class AuthRoutes(val cfg: Config, val db: Database)(implicit val executor: ExecutionContext) extends Routes {
  val um = new UserManager(db)

  def route =
    path("login") {
      post {
        entity(as[UserPass]) { userPass =>
          onSuccess(authenticator.userPassAuthenticator(Some(userPass))) {
            case Some(user) =>
              import BearerTokenHandler._
              val token = serialize(sign(generate(user, None), cfg.httpAuthBearerTokenServerSecret))
              complete(AuthenticationResponse("Authenticated", Some(user), Some(token)))
            case _ => complete(AuthenticationResponse("Unauthenticated"))
          }
        }
      }
    } ~
    path("state") {
      get {
        authenticateOption() {
          case Some(user) => complete(AuthenticationResponse("Authenticated", Some(user)))
          case _ => complete(AuthenticationResponse("Unauthenticated"))
        }
      }
    } ~
    path("register") {
      post {
        entity(as[RegistrationRequest]) { registration =>
          onSuccess(um.createUser(User(userName = registration.userName), registration.password)) { user =>
            complete(user)
          }
        }
      }
    }
}
