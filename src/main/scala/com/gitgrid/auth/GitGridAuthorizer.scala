package com.gitgrid.auth

import com.gitgrid.models._
import scala.concurrent.Future

case class AccessProjectRepository(project: Project)

class GitGridAuthorizer(db: Database) {
  def authorize(user: Option[User], action: => Any): Future[Boolean] = action match {
    case AccessProjectRepository(project) =>
      user match {
        case Some(user) => Future.successful(project.userId == user.id.get)
        case _ => Future.successful(false)
      }
    case _ =>
      Future.failed[Boolean](new Exception(s"Unknown authorization request for $action"))
  }
}
