package com.gitgrid.http.routes

import java.util.Date
import com.gitgrid.Config
import com.gitgrid.auth._
import com.gitgrid.managers.UserManager
import com.gitgrid.models._
import spray.routing.authentication._
import scala.concurrent._

case class AuthenticationRequest(userName: String, password: String, expiresAt: Option[Date] = None)
case class AuthenticationResponse(message: String, user: Option[User] = None, token: Option[String] = None)
case class RegistrationRequest(userName: String, password: String)

class AuthRoutes(val cfg: Config, val db: Database)(implicit val executor: ExecutionContext) extends Routes {
  val um = new UserManager(db)

  def route =
    path("login") {
      post {
        entity(as[AuthenticationRequest]) { case AuthenticationRequest(userName, password, requestedExpiresAt) =>
          onSuccess(authenticator.userPassAuthenticator(Some(UserPass(userName, password)))) {
            case Some(user) =>
              import BearerTokenHandler._
              val serverExpiresAt = new Date(System.currentTimeMillis + cfg.httpAuthBearerTokenMaximalLifetime)
              val expiresAt = minDate(serverExpiresAt, requestedExpiresAt)
              val token = serialize(sign(generate(user, Some(expiresAt)), cfg.httpAuthBearerTokenServerSecret))
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

  private def minDate(a: Date, b: Option[Date]): Date = b.map(b => if (a.before(b)) a else b).getOrElse(a)
}
