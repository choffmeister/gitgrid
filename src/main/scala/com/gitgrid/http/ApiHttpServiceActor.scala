package com.gitgrid.http

import akka.actor._
import com.gitgrid.Config
import com.gitgrid.auth._
import com.gitgrid.http.directives._
import com.gitgrid.managers.UserManager
import com.gitgrid.models._
import spray.routing._
import spray.routing.authentication.UserPass

case class AuthenticationResponse(message: String, user: Option[User])
case class AuthenticationState(user: Option[User])

class ApiHttpServiceActor(implicit config: Config) extends Actor with ActorLogging with HttpService with AuthenticationDirectives with JsonProtocol {
  implicit val actorRefFactory = context
  implicit val executor = context.dispatcher
  val db = Database()
  val auth = new GitGridHttpAuthenticator(db)

  def receive = runRoute(route)
  lazy val route =
    pathPrefix("api") {
      pathPrefix("auth") {
        authRoute
      } ~
      path("ping") {
        complete("pong")
      }
    }

  lazy val authRoute =
    path("login") {
      post {
        formsLogin(auth) {
          case Some(user) => complete(AuthenticationResponse("Logged in", Some(user)))
          case _ => complete(AuthenticationResponse("Invalid username or password", None))
        }
      }
    } ~
    path("logout") {
      post {
        formsLogout(auth) {
          complete(AuthenticationResponse("Logged out", None))
        }
      }
    } ~
    path("state") {
      get {
        authenticateOption(auth) { user =>
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
}
