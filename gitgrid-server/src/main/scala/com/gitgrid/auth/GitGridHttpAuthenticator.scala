package com.gitgrid.auth

import com.gitgrid._
import com.gitgrid.auth.RichHttpAuthenticator._
import com.gitgrid.http.JsonProtocol
import com.gitgrid.managers._
import com.gitgrid.models._
import reactivemongo.bson.BSONObjectID
import spray.routing._
import spray.routing.authentication._

import scala.concurrent._

class GitGridHttpAuthenticator(coreConf: CoreConfig, httpConf: HttpConfig, db: Database)(implicit executionContext: ExecutionContext) extends ContextAuthenticator[User] with JsonProtocol {
  val userManager = new UserManager(coreConf, db)
  val userPassAuthenticator =  new GitGridUserPassAuthenticator(httpConf, userManager)
  val bearerTokenAuthenticator = new OAuth2BearerTokenAuthenticator[User](httpConf.authRealm, httpConf.authBearerTokenSecret, id => db.users.find(BSONObjectID(id)))
  val basicAuthenticator = new BasicHttpAuthenticator[User](httpConf.authRealm, userPassAuthenticator)
  val authenticator = bearerTokenAuthenticator.withFallback(basicAuthenticator)

  def apply(ctx: RequestContext): Future[Authentication[User]] = authenticator(ctx)
}
