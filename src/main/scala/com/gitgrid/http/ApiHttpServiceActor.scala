package com.gitgrid.http

import akka.actor._
import com.gitgrid.Config
import com.gitgrid.auth._
import com.gitgrid.git.GitRepository
import com.gitgrid.http.directives._
import com.gitgrid.managers.UserManager
import com.gitgrid.models._
import java.io.File
import spray.routing._
import spray.routing.authentication.UserPass

case class AuthenticationResponse(message: String, user: Option[User])
case class AuthenticationState(user: Option[User])

class ApiHttpServiceActor(db: Database) extends Actor with ActorLogging with HttpService with AuthenticationDirectives with JsonProtocol {
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
      pathPrefix("projects") {
        projectRoute
      }
    }

  lazy val projectRoute =
    pathEnd {
      post {
        authenticate() { user =>
          entity(as[Project]) { project =>
            onSuccess(db.projects.insert(project.copy(id = Some(reactivemongo.bson.BSONObjectID.generate), userId = user.id.get))) { project =>
              val dir = new File(Config.repositoriesDir, project.id.get.stringify)
              GitRepository.init(dir, bare = true)
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
