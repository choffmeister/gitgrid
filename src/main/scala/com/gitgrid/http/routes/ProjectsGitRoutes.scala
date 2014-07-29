package com.gitgrid.http.routes

import com.gitgrid.Config
import com.gitgrid.git._
import com.gitgrid.managers._
import com.gitgrid.models._

import scala.concurrent._

class ProjectsGitRoutes(val cfg: Config, val db: Database)(implicit val executor: ExecutionContext) extends Routes {
  val pm = new ProjectManager(cfg, db)

  def route(project: Project) =
    path("branches") {
      complete(gitRepository(project)(repo => repo.branches()))
    } ~
    path("tags") {
      complete(gitRepository(project)(repo => repo.tags()))
    } ~
    path("commits" / Segment) { refOrSha =>
      complete(gitRepository(project)(repo => repo.commits(repo.resolve(refOrSha))))
    } ~
    path("commit" / Segment) { refOrSha =>
      complete(gitRepository(project)(repo => repo.commit(repo.resolve(refOrSha))))
    } ~
    path("tree" / Segment) { sha =>
      complete(gitRepository(project)(repo => repo.tree(repo.resolve(sha))))
    } ~
    path("blob" / Segment) { sha =>
      complete(gitRepository(project)(repo => repo.blob(repo.resolve(sha))))
    } ~
    path("blob-raw" / Segment) { sha =>
      complete(gitRepository(project)(repo => repo.blob(repo.resolve(sha)).readAsString(repo)))
    } ~
    path("tree" / Segment / RestPath) { (refOrSha, path) =>
      complete {
        gitRepository(project) { repo =>
          val commitId = repo.resolve(refOrSha)
          val commit = repo.commit(commitId)
          val tree = repo.traverse(commit, "/" + path).asInstanceOf[GitTree]
          tree
        }
      }
    } ~
    path("blob" / Segment / RestPath) { (refOrSha, path) =>
      complete {
        gitRepository(project) { repo =>
          val commitId = repo.resolve(refOrSha)
          val commit = repo.commit(commitId)
          val blob = repo.traverse(commit, "/" + path).asInstanceOf[GitBlob]
          blob
        }
      }
    } ~
    path("blob-raw"/ Segment / RestPath) { (refOrSha, path) =>
      complete {
        gitRepository(project) { repo =>
          val commitId = repo.resolve(refOrSha)
          val commit = repo.commit(commitId)
          val blob = repo.traverse(commit, "/" + path).asInstanceOf[GitBlob]
          blob.readAsString(repo)
        }
      }
    }

  def gitRepository[T](project: Project)(inner: GitRepository => T): T =
    GitRepository(pm.getRepositoryDirectory(project.id))(inner)
}
