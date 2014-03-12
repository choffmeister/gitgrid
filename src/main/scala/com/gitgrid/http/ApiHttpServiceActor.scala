package com.gitgrid.http

import akka.actor._
import com.gitgrid.Config
import com.gitgrid.auth.AuthenticationHandler
import com.gitgrid.http.directives._
import com.gitgrid.models._
import scala.concurrent.Future
import spray.routing.HttpService

case class AuthenticationRequest(userName: String, password: String)
case class AuthenticationResponse(message: String, user: Option[User])
case class AuthenticationState(user: Option[User])

class ApiHttpServiceActor(implicit config: Config) extends Actor with ActorLogging with HttpService with AuthenticationDirectives {
  import JsonProtocol._

  implicit val actorRefFactory = context
  implicit val executor = context.dispatcher
  val auth = new AuthenticationHandler()

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
        entity(as[AuthenticationRequest]) { credentials =>
          val future = auth
            .authenticate(credentials.userName, credentials.password)
            .flatMap[Option[(User, Session)]] {
              case Some(user) => auth.createSession(user.id.get).map(s => Some((user, s)))
              case _ => Future.successful(None)
            }

          onSuccess(future) {
            case Some((user, session)) =>
              createSessionCookie(session) {
                complete(AuthenticationResponse("Logged in", Some(user)))
              }
            case _ =>
              complete(AuthenticationResponse("Invalid username or password", None))
          }
        }
      }
    } ~
    path("logout") {
      post {
        removeSessionCookie() {
          extractSessionId {
            case Some(sessionId) =>
              onSuccess(auth.revokeSession(sessionId)) {
                case _ => complete(AuthenticationState(None))
              }
            case _ =>
              complete(AuthenticationState(None))
          }
        }
      }
    } ~
    path("state") {
      get {
        authenticateOption { user =>
          complete(AuthenticationState(user))
        }
      }
    }
}
