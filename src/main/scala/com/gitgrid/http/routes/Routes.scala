package com.gitgrid.http.routes

import com.gitgrid.Config
import com.gitgrid.auth._
import com.gitgrid.http.JsonProtocol
import com.gitgrid.http.directives._
import com.gitgrid.models._
import scala.concurrent.ExecutionContext
import spray.routing._

trait Routes extends Directives with AuthenticationDirectives with ExtractionDirectives with JsonProtocol {
  implicit val cfg: Config
  implicit val db: Database
  implicit val executor: ExecutionContext

  val authenticator = new GitGridHttpAuthenticator(cfg, db)

  def authenticate(): Directive1[User] = authenticate(authenticator)
  def authenticateOption(): Directive1[Option[User]] = authenticateOption(authenticator)
}
