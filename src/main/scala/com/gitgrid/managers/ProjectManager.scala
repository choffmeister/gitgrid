package com.gitgrid.managers

import com.gitgrid.Config
import com.gitgrid.git.GitRepository
import com.gitgrid.models._
import java.io.File
import reactivemongo.bson._
import scala.concurrent._

class ProjectManager(cfg: Config, db: Database)(implicit ec: ExecutionContext) {
  def createProject(project: Project): Future[Project] = {
    val now = BSONDateTime(System.currentTimeMillis)
    val project2 = project.copy(createdAt = now, updatedAt = now, pushedAt = None)
    for {
      project <- db.projects.insert(project2)
      repository <- future(GitRepository.init(getRepositoryDirectory(project.id.get), bare = true))
    } yield project
  }

  def getRepositoryDirectory(projectId: BSONObjectID): File = new File(cfg.repositoriesDir, projectId.stringify)
}
