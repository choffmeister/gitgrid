package com.gitgrid.http.routes

import com.gitgrid._
import com.gitgrid.managers._
import com.gitgrid.models._
import shapeless._
import spray.routing._

import scala.concurrent._

class ProjectsRoutes(val coreConf: CoreConfig, val httpConf: HttpConfig, val db: Database)(implicit val executor: ExecutionContext) extends Routes {
  val gitRoutes = new ProjectsGitRoutes(coreConf, httpConf, db)
  val pm = new ProjectManager(coreConf, db)

  def route =
    authorizeProject { (user, project) =>
      pathEnd {
        complete(project)
      } ~
      pathPrefix("git") {
        gitRoutes.route(project)
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
    extract(ctx => ctx).flatMap { ctx =>
      val d = for {
        ownerName <- pathPrefix(Segment)
        projectName <- pathPrefix(Segment)
        user <- onSuccess(authenticator(ctx))
        owner <- onSuccess(db.users.findByUserName(ownerName))
        project <- onSuccess(db.projects.findByFullQualifiedName(ownerName, projectName))
      } yield (ownerName, projectName, user, owner, project)

      d.flatMap {
        case (_, _, _, None, _) => reject
        case (_, _, Right(u), Some(o), Some(p)) if o.id == u.id => hprovide(Some(u) :: p :: HNil)
        case (on, _, Right(u), _, None) if u.userName == on => reject
        case (_, _, Right(u), _, Some(p)) if p.public => hprovide(Some(u) :: p :: HNil)
        case (_, _, Left(r), _, Some(p)) if p.public => hprovide(None :: p :: HNil)
        case (_, _, Right(u), _, _) => reject(AuthorizationFailedRejection)
        case (_, _, Left(r), _, _) => reject(r)
      }
    }
  }
}
