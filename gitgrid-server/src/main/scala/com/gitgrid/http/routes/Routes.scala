package com.gitgrid.http.routes

import com.gitgrid._
import com.gitgrid.auth._
import com.gitgrid.http.JsonProtocol
import com.gitgrid.http.directives._
import com.gitgrid.models._
import spray.routing._

import scala.concurrent.ExecutionContext

trait Routes extends Directives with AuthenticationDirectives with ExtractionDirectives with JsonProtocol {
  implicit val coreConf: CoreConfig
  implicit val httpConf: HttpConfig
  implicit val db: Database
  implicit val executor: ExecutionContext

  val authenticator = new GitGridHttpAuthenticator(coreConf, httpConf, db)

  def authenticate(): Directive1[User] = authenticate(authenticator)
  def authenticateOption(): Directive1[Option[User]] = authenticateOption(authenticator)
}
