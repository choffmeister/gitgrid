package com.gitgrid.http

import akka.actor._
import com.gitgrid.Config
import com.gitgrid.auth.{SessionHandler, AuthenticationHandler}
import com.gitgrid.http.directives._
import com.gitgrid.models._
import scala.concurrent.Future
import spray.routing.HttpService

case class AuthenticationRequest(userName: String, password: String)
case class AuthenticationResponse(message: String, user: Option[User])

class ApiHttpServiceActor(implicit config: Config) extends Actor with ActorLogging with HttpService with AuthenticationDirectives {
  import JsonProtocol._

  implicit val actorRefFactory = context
  implicit val executor = context.dispatcher
  val authenticationHandler = new AuthenticationHandler()
  val sessionHandler = new SessionHandler()

  def receive = runRoute(route)
  lazy val route =
    pathPrefix("api") {
      pathPrefix("auth") {
        authRoute
      } ~
      path("ping") {
        post {
          complete("pong")
        }
      }
    }

  lazy val authRoute =
    path("login") {
      post {
        entity(as[AuthenticationRequest]) { credentials =>
          val future = authenticationHandler
            .authenticate(credentials.userName, credentials.password)
            .flatMap[Option[(User, Session)]] { user =>
              user match {
                case Some(user) =>
                  sessionHandler.createSession(user.id.get).map(s => Some((user, s)))
                case _ => Future.successful(None)
              }
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
          extractSessionId { (sessionId: Option[String]) =>
            sessionId match {
              case Some(sessionId) =>
                onSuccess(sessionHandler.revokeSession(sessionId)) {
                  case _ => complete("Logout")
                }
              case _ =>
                complete("Logout")
            }
          }
        }
      }
    }
}
