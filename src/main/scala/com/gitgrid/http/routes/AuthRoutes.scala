package com.gitgrid.http.routes

import java.util.Date

import com.gitgrid.Config
import com.gitgrid.auth._
import com.gitgrid.http.JsonProtocol
import com.gitgrid.managers.UserManager
import com.gitgrid.models._
import spray.routing.Route
import spray.routing.authentication._

import scala.concurrent._

/**
 * See http://tools.ietf.org/html/rfc6749.
 */
case class OAuth2AccessTokenResponse(tokenType: String, accessToken: String, expiresIn: Long)
case class RegistrationRequest(userName: String, email: String, password: String)

class AuthRoutes(val cfg: Config, val db: Database)(implicit val executor: ExecutionContext) extends Routes with JsonProtocol {
  val um = new UserManager(cfg, db)

  def route =
    pathPrefix("token") {
      path("create") {
        authenticate(authenticator.basicAuthenticator) { user =>
          completeWithToken(user)
        }
      } ~
      path("renew") {
        extract(ctx => ctx.request) { req =>
          authenticator.bearerTokenAuthenticator.extractToken(req) match {
            case Right(token) =>
              if (!token.validate(cfg.httpAuthBearerTokenServerSecret)) reject()
              else completeWithToken(token.payload)
            case _ => reject()
          }
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
        entity(as[RegistrationRequest]) { reg =>
          onSuccess(um.createUser(User(userName = reg.userName, email = reg.email), reg.password)) { user =>
            complete(user)
          }
        }
      }
    }

  private def completeWithToken(user: User): Route = {
    val expiresAt = new Date(System.currentTimeMillis + cfg.httpAuthBearerTokenMaximalLifetime.toMillis)
    val token = OAuth2BearerTokenTyped.create(user, expiresAt).sign(cfg.httpAuthBearerTokenServerSecret)
    val tokenStr = OAuth2BearerTokenSerializer.serialize(token)
    complete(OAuth2AccessTokenResponse("bearer", tokenStr, cfg.httpAuthBearerTokenMaximalLifetime.toSeconds))
  }
}
