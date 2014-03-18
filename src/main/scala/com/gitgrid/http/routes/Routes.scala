package com.gitgrid.http.routes

import com.gitgrid.auth._
import com.gitgrid.http.JsonProtocol
import com.gitgrid.http.directives._
import com.gitgrid.models.Database
import scala.concurrent.ExecutionContext
import spray.routing._

trait Routes extends Directives with AuthenticationDirectives with ExtractionDirectives with JsonProtocol {
  implicit val db: Database
  implicit val executor: ExecutionContext

  val authenticator = new GitGridHttpAuthenticator(db)
  val authorizer = new GitGridAuthorizer(db)
}
