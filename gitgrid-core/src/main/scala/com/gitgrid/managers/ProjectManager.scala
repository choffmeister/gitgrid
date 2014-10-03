package com.gitgrid.managers

import java.io.File

import com.gitgrid.Config
import com.gitgrid.git.GitRepository
import com.gitgrid.models._
import reactivemongo.bson._

import scala.concurrent._

class ProjectManager(cfg: Config, db: Database)(implicit ec: ExecutionContext) {
  def createProject(project: Project): Future[Project] = {
    for {
      project <- db.projects.insert(project)
      repository <- future(GitRepository.init(getRepositoryDirectory(project.id), bare = true))
    } yield project
  }

  def listProjects(authUser: Option[User]): Future[List[Project]] = authUser match {
    case Some(authUser) =>
      db.projects.query(BSONDocument("$or" -> List(BSONDocument("ownerId" -> authUser.id), BSONDocument("public" -> true))))
    case _ =>
      db.projects.query(BSONDocument("public" -> true))
  }

  def listProjectsForOwner(authUser: Option[User], ownerId: BSONObjectID): Future[List[Project]] = authUser match {
    case Some(authUser) if authUser.id == ownerId =>
      db.projects.query(BSONDocument("ownerId" -> authUser.id))
    case _ =>
      db.projects.query(BSONDocument("$and" -> List(BSONDocument("ownerId" -> ownerId),  BSONDocument("public" -> true))))
  }

  def getRepositoryDirectory(projectId: BSONObjectID): File = new File(cfg.repositoriesDir, projectId.stringify)
}
