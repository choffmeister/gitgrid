package com.gitgrid.auth

import com.gitgrid.Config
import com.gitgrid.managers._
import com.gitgrid.models._
import com.gitgrid.http.JsonProtocol
import spray.http.HttpChallenge
import spray.http.HttpHeaders.`WWW-Authenticate`
import scala.concurrent._
import spray.routing._
import spray.routing.authentication._

class GitGridHttpAuthenticator(cfg: Config, db: Database)(implicit executionContext: ExecutionContext) extends ContextAuthenticator[User] with JsonProtocol {
  val userManager = new UserManager(db)
  val userPassAuthenticator =  new GitGridUserPassAuthenticator(userManager)
  val bearerTokenAuthenticator = new OAuth2BearerTokenAuthenticator[User](cfg.httpAuthRealm, cfg.httpAuthBearerTokenServerSecret)
  val basicAuthenticator = new EnhancedBasicHttpAuthenticator[User](cfg.httpAuthRealm, userPassAuthenticator)
  val authenticator = EnhancedHttpAuthenticator.combine(bearerTokenAuthenticator, basicAuthenticator)

  def apply(ctx: RequestContext): Future[Authentication[User]] = authenticator(ctx).map {
    // filter out HTTP basic challenges to prevent interactive window in browsers
    case Left(AuthenticationFailedRejection(c, ch)) =>
      Left(AuthenticationFailedRejection(c, ch.map {
        case `WWW-Authenticate`(ch) =>
          `WWW-Authenticate`(ch.filter {
            case HttpChallenge("Basic", _, _) => false
            case _ => true
          })
        case x => x
      } filter {
        case `WWW-Authenticate`(Nil) => false
        case _ => true
      }))
    case x => x
  }
}
