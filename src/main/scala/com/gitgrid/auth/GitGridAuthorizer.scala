package com.gitgrid.auth

import com.gitgrid.models._
import scala.concurrent.Future

case class ProjectRead(project: Project)
case class ProjectRepositoryReadWrite(project: Project)

class GitGridAuthorizer(db: Database) {
  def authorize(user: Option[User], action: => Any): Future[Boolean] = action match {
    case ProjectRead(project) =>
      if (project.public == false) {
        user match {
          case Some(user) => Future.successful(project.ownerId == user.id.get)
          case _ => Future.successful(false)
        }
      } else Future.successful(true)
    case ProjectRepositoryReadWrite(project) =>
      authorize(user, ProjectRead(project))
    case _ =>
      Future.failed[Boolean](new Exception(s"Unknown authorization request for $action"))
  }
}
