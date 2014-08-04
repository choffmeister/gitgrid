package com.gitgrid.http.routes

import java.util.Date

import com.gitgrid.Config
import com.gitgrid.auth.OAuth2BearerTokenAuthenticator.TokenManipulated
import com.gitgrid.auth._
import com.gitgrid.http.JsonProtocol
import com.gitgrid.models._
import spray.routing.Route

import scala.concurrent._

class AuthRoutes(val cfg: Config, val db: Database)(implicit val executor: ExecutionContext) extends Routes with JsonProtocol {
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
            case Right((header, token, signature)) =>
              if (!JsonWebToken.checkSignature(header, token, signature, cfg.httpAuthBearerTokenSecret))
                reject(authenticator.bearerTokenAuthenticator.createRejection(TokenManipulated))
              else
                completeWithToken(token)
            case Left(rejection) => reject(rejection)
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
    }

  private def completeWithToken(token: JsonWebToken): Route = {
    val now = System.currentTimeMillis
    val header = JoseHeader(
      algorithm = "HS256"
    )
    val token2 = token.copy(
      createdAt = new Date(now),
      expiresAt = new Date(now + cfg.httpAuthBearerTokenLifetime.toMillis)
    )
    val signature = JsonWebToken.createSignature(header, token2, cfg.httpAuthBearerTokenSecret)
    complete(OAuth2AccessTokenResponse("bearer", JsonWebToken.write(header, token2, signature), cfg.httpAuthBearerTokenLifetime.toSeconds))
  }

  private def completeWithToken(user: User): Route = {
    completeWithToken(JsonWebToken(
      createdAt = new Date(0),
      expiresAt = new Date(0),
      subject = user.id.stringify,
      name = user.userName,
      payload = Map.empty
    ))
  }
}
