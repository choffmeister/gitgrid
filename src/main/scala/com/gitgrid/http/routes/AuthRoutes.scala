package com.gitgrid.http.routes

import java.util.Date
import com.gitgrid.Config
import com.gitgrid.auth._
import com.gitgrid.managers.UserManager
import com.gitgrid.models._
import com.gitgrid.http.JsonProtocol
import spray.routing.authentication._
import spray.routing.Route
import scala.concurrent._

case class AuthenticationRequest(userName: String, password: String, expiresAt: Option[Date] = None)
case class AuthenticationResponse(message: String, user: Option[User] = None, token: Option[String] = None)
case class RegistrationRequest(userName: String, password: String)

class AuthRoutes(val cfg: Config, val db: Database)(implicit val executor: ExecutionContext) extends Routes with JsonProtocol {
  val um = new UserManager(db)

  def route =
    path("login") {
      post {
        entity(as[AuthenticationRequest]) { case AuthenticationRequest(userName, password, requestedExpiresAt) =>
          onSuccess(authenticator.userPassAuthenticator(Some(UserPass(userName, password)))) {
            case Some(user) => completeAuthentication(user, requestedExpiresAt)
            case _ => complete(AuthenticationResponse("Unauthenticated"))
          }
        }
      }
    } ~
    path("renew") {
      extract(ctx => ctx.request) { req =>
        authenticator.bearerTokenAuthenticator.extractToken(req) match {
          case Right(token) =>
            if (!token.validate(cfg.httpAuthBearerTokenServerSecret)) reject()
            else completeAuthentication(token.payload)
          case _ => reject()
        }
      }
    } ~
    path("state") {
      get {
        authenticate() { user =>
          complete(user)
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

  private def completeAuthentication(user: User, requestedExpiresAt: Option[Date] = None): Route = {
    val serverExpiresAt = new Date(System.currentTimeMillis + cfg.httpAuthBearerTokenMaximalLifetime)
    val expiresAt = minDate(serverExpiresAt, requestedExpiresAt)
    val token = OAuth2BearerTokenTyped.create(user, expiresAt).sign(cfg.httpAuthBearerTokenServerSecret)
    val tokenStr = OAuth2BearerTokenSerializer.serialize(token)
    complete(AuthenticationResponse("Authenticated", Some(user), Some(tokenStr)))
  }
  private def minDate(a: Date, b: Option[Date]): Date = b.map(b => if (a.before(b)) a else b).getOrElse(a)
}
