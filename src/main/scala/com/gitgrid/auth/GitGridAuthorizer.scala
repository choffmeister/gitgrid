package com.gitgrid.auth

import com.gitgrid.models._
import scala.concurrent.Future

class GitGridAuthorizer(db: Database) {
  def authorize(user: Option[User], action: => Any): Future[Boolean] = action match {
    case _ =>
      Future.failed[Boolean](new Exception(s"Unknown authorization request for $action"))
  }
}
