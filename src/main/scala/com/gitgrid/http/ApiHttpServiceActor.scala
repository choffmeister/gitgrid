package com.gitgrid.http

import akka.actor._
import com.gitgrid.Config
import com.gitgrid.auth._
import com.gitgrid.git._
import com.gitgrid.http.directives._
import com.gitgrid.managers.{ProjectManager, UserManager}
import com.gitgrid.models._
import java.io.File
import spray.routing._
import spray.routing.authentication.UserPass

case class AuthenticationResponse(message: String, user: Option[User])
case class AuthenticationState(user: Option[User])

class ApiHttpServiceActor(val db: Database) extends Actor with ActorLogging with HttpService with AuthenticationDirectives with ExtractionDirectives with JsonProtocol {
  implicit val actorRefFactory = context
  implicit val executor = context.dispatcher
  val authenticator = new GitGridHttpAuthenticator(db)
  val authorizer = new GitGridAuthorizer(db)

  def receive = runRoute(route)
  lazy val route =
    pathPrefix("api") {
      pathPrefix("auth") {
        authRoute
      } ~
      path("ping") {
        complete("pong")
      } ~
      pathPrefix("users") {
        usersRoute
      } ~
      pathPrefix("projects") {
        projectsRoute
      }
    }

  lazy val usersRoute =
    userPathPrefix { user =>
      complete(user)
    }

  lazy val projectsRoute =
    projectPathPrefix { project =>
      pathEnd {
        complete(project)
      } ~
      pathPrefix("git") {
        def gitRepository[T](project: Project)(inner: GitRepository => T): T =
          GitRepository(new File(Config.repositoriesDir, project.id.get.stringify))(inner)

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
    pathEnd {
      post {
        authenticate() { user =>
          entity(as[Project]) { project =>
            val pm = new ProjectManager(db)
            onSuccess(pm.createProject(user.id.get, project.name)) { project =>
              complete(project)
            }
          }
        }
      }
    }

  lazy val authRoute =
    path("login") {
      post {
        formsLogin(authenticator) {
          case Some(user) => complete(AuthenticationResponse("Logged in", Some(user)))
          case _ => complete(AuthenticationResponse("Invalid username or password", None))
        }
      }
    } ~
    path("logout") {
      post {
        formsLogout(authenticator) {
          complete(AuthenticationResponse("Logged out", None))
        }
      }
    } ~
    path("state") {
      get {
        authenticateOption() { user =>
          complete(AuthenticationState(user))
        }
      }
    } ~
    path("register") {
      post {
        entity(as[UserPass]) { userPass =>
          val um = new UserManager(db)
          onSuccess(um.createUser(userPass.user, userPass.pass)) { user => complete(user) }
        }
      }
    }

  def authenticate(): Directive1[User] = authenticate(authenticator)
  def authenticateOption(): Directive1[Option[User]] = authenticateOption(authenticator)
  def authorize(user: Option[User], action: => Any): Directive0 = authorizeDetached(authorizer.authorize(user, action))
  def authorize(user: User, action: => Any): Directive0 = authorizeDetached(authorizer.authorize(Some(user), action))
}
