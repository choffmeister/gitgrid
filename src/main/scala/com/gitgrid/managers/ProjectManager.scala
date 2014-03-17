package com.gitgrid.managers

import com.gitgrid.Config
import com.gitgrid.git.GitRepository
import com.gitgrid.models._
import java.io.File
import reactivemongo.bson.BSONObjectID
import scala.concurrent._

class ProjectManager(db: Database)(implicit ec: ExecutionContext) {
  def createProject(userId: BSONObjectID, name: String): Future[Project] = {
    for {
      project <- db.projects.insert(Project(userId = userId, name = name))
      repository <- future(GitRepository.init(new File(Config.repositoriesDir, project.id.get.stringify), bare = true))
    } yield project
  }
}
