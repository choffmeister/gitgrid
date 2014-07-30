package com.gitgrid.auth

import com.gitgrid.Config
import com.gitgrid.http.JsonProtocol
import com.gitgrid.managers._
import com.gitgrid.models._
import spray.routing._
import spray.routing.authentication._

import scala.concurrent._

class GitGridHttpAuthenticator(cfg: Config, db: Database)(implicit executionContext: ExecutionContext) extends ContextAuthenticator[User] with JsonProtocol {
  val userManager = new UserManager(cfg, db)
  val userPassAuthenticator =  new GitGridUserPassAuthenticator(cfg, userManager)
  val bearerTokenAuthenticator = new OAuth2BearerTokenAuthenticator[User](cfg.httpAuthRealm, cfg.httpAuthBearerTokenServerSecret)
  val basicAuthenticator = new EnhancedBasicHttpAuthenticator[User](cfg.httpAuthRealm, userPassAuthenticator)
  val authenticator = EnhancedHttpAuthenticator.combine(bearerTokenAuthenticator, basicAuthenticator)

  def apply(ctx: RequestContext): Future[Authentication[User]] = authenticator(ctx)
}
