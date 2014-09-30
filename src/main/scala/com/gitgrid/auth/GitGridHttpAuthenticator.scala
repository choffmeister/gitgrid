package com.gitgrid.auth

import com.gitgrid.auth.RichHttpAuthenticator._
import com.gitgrid.Config
import com.gitgrid.http.JsonProtocol
import com.gitgrid.managers._
import com.gitgrid.models._
import reactivemongo.bson.BSONObjectID
import spray.routing._
import spray.routing.authentication._

import scala.concurrent._

class GitGridHttpAuthenticator(cfg: Config, db: Database)(implicit executionContext: ExecutionContext) extends ContextAuthenticator[User] with JsonProtocol {
  val userManager = new UserManager(cfg, db)
  val userPassAuthenticator =  new GitGridUserPassAuthenticator(cfg, userManager)
  val bearerTokenAuthenticator = new OAuth2BearerTokenAuthenticator[User](cfg.httpAuthRealm, cfg.httpAuthBearerTokenSecret, id => db.users.find(BSONObjectID(id)))
  val basicAuthenticator = new BasicHttpAuthenticator[User](cfg.httpAuthRealm, userPassAuthenticator)
  val authenticator = bearerTokenAuthenticator.withFallback(basicAuthenticator)

  def apply(ctx: RequestContext): Future[Authentication[User]] = authenticator(ctx)
}
