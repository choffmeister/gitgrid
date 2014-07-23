package com.gitgrid.http.routes

import com.gitgrid.Config
import com.gitgrid.git._
import com.gitgrid.managers._
import com.gitgrid.models._
import spray.routing._
import scala.concurrent._
import shapeless._
import java.io.File

class ProjectsRoutes(val cfg: Config, val db: Database)(implicit val executor: ExecutionContext) extends Routes {
  val pm = new ProjectManager(cfg, db)

  def route =
    authorizeProject { (user, project) =>
      pathEnd {
        complete(project)
      } ~
      pathPrefix("git") {
        path("branches") {
          complete(gitRepository(project)(repo => repo.branches()))
        } ~
        path("tags") {
          complete(gitRepository(project)(repo => repo.tags()))
        } ~
        path("commits") {
          complete(gitRepository(project)(repo => repo.commits()))
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
      }
    } ~
    userPathPrefix { user =>
      pathEnd {
        get {
          authenticateOption() { authUser =>
            complete(pm.listProjectsForOwner(authUser, user.id))
          }
        }
      }
    } ~
    pathEnd {
      get {
        authenticateOption() { authUser =>
          complete(pm.listProjects(authUser))
        }
      } ~
      post {
        authenticate() { user =>
          entity(as[Project]) { project =>
            authorize(project.ownerId == user.id) {
              onSuccess(pm.createProject(project)) { project =>
                complete(project)
              }
            }
          }
        }
      }
    }

  def authorizeProject: Directive[::[Option[User], ::[Project, HNil]]] = {
    val d = for {
      user <- authenticateOption()
      ownerName <- pathPrefix(Segment)
      projectName <- pathPrefix(Segment)
      owner <- onSuccess(db.users.findByUserName(ownerName))
      project <- onSuccess(db.projects.findByFullQualifiedName(ownerName, projectName))
    } yield (user, ownerName, projectName, owner, project)

    d.flatMap {
      case (_, _, _, None, _) => reject
      case (Some(u), _, _, Some(o), Some(p)) if o.id == u.id => hprovide(Some(u) :: p :: HNil)
      case (Some(u), on, _, _, None) if u.userName == on => reject
      case (u, _, _, _, Some(p)) if p.public => hprovide(u :: p :: HNil)
      case (Some(u), _, _, _, _) => reject(AuthorizationFailedRejection)
      case (None, _, _, _, _) => reject(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, Nil))
    }
  }

  def gitRepository[T](project: Project)(inner: GitRepository => T): T =
    GitRepository(new File(cfg.repositoriesDir, project.id.stringify))(inner)
}
