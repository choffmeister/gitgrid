package com.gitgrid.http.routes

import com.gitgrid.auth._
import com.gitgrid.http.JsonProtocol
import com.gitgrid.http.directives._
import com.gitgrid.models._
import scala.concurrent.ExecutionContext
import spray.routing._

trait Routes extends Directives with AuthenticationDirectives with ExtractionDirectives with JsonProtocol {
  implicit val db: Database
  implicit val executor: ExecutionContext

  val authenticator = new GitGridHttpAuthenticator(db)
  val authorizer = new GitGridAuthorizer(db)

  def authenticate(): Directive1[User] = authenticate(authenticator)
  def authenticateOption(): Directive1[Option[User]] = authenticateOption(authenticator)
  def authorize(user: Option[User], action: Any): Directive0 = authorizeDetached(authorizer.authorize(user, action))
}
