package com.gitgrid.http.directives

import com.gitgrid.models._
import scala.concurrent.ExecutionContext
import spray.routing.Directive1
import spray.routing.Directives._

trait ExtractionDirectives {
  implicit val db: Database
  implicit val executor: ExecutionContext

  def userPathPrefix: Directive1[User] =
    for {
      userName <- pathPrefix(Segment)
      userOption <- onSuccess(db.users.findByUserName(userName))
      user <- provideNonOption(userOption)
    } yield user

  def projectPathPrefix: Directive1[Project] =
    for {
      ownerName <- pathPrefix(Segment)
      projectName <- pathPrefix(Segment)
      projectOption <- onSuccess(db.projects.findByFullQualifiedName(ownerName, projectName))
      project <- provideNonOption(projectOption)
    } yield project

  def provideNonOption[T](option: Option[T]): Directive1[T] =
    option match {
      case Some(value) => provide(value)
      case _ => reject
    }
}
