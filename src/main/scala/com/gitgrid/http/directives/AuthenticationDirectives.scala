package com.gitgrid.http.directives

import com.gitgrid.auth._
import com.gitgrid.models._
import scala.concurrent._
import shapeless.HNil
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.routing.Directives._
import spray.routing._
import spray.routing.authentication.UserPass
import spray.routing.directives.AuthMagnet

trait AuthenticationDirectives {
  implicit val executor: ExecutionContext

  def authenticateOption[T](magnet: AuthMagnet[T]): Directive1[Option[T]] = authenticate(magnet)
    .flatMap(user => provide(Some(user)))
    .recover {
      case _ => provide(Option.empty[T])
    }

  def authorizeDetached(check: => Future[Boolean]): Directive0 = onSuccess(check)
    .flatMap[HNil] {
      res => authorize(res)
    }

  def authorizeDetached(check: RequestContext => Future[Boolean]): Directive0 = extract(check)
    .flatMap { future =>
      onSuccess(future).flatMap[HNil] {
        res => authorize(res)
      }
    }

  def formsLogin(auth: GitGridHttpAuthenticator): Directive1[Option[User]] = {
    implicit val stringFormat = DefaultJsonProtocol.StringJsonFormat
    implicit val userPassFormat = DefaultJsonProtocol.jsonFormat2(UserPass)
    implicit val userPassUnmarshaller = SprayJsonSupport.sprayJsonUnmarshaller[UserPass](userPassFormat)

    entity(as[UserPass]).flatMap { userPass =>
      val future = auth.userPassAuthenticator(Some(userPass)).flatMap {
        case Some(user) => auth.sessionHandler.createSession(user.id.get).map(session => Some(user, session))
        case _ => Future.successful(Option.empty[(User, Session)])
      }

      onSuccess(future).flatMap {
        case Some((user, session)) => createSessionCookie(session, auth.cookieName, auth.cookiePath).hflatMap { case _ => hprovide(Some(user) :: HNil) }
        case _ => provide(None)
      }
    }
  }

  def formsLogout(auth: GitGridHttpAuthenticator): Directive0 = {
    extract(ctx => ctx).flatMap { ctx =>
      val future = ctx.request.cookies.find(c => c.name == auth.cookieName).map(_.content) match {
        case Some(sessionId) => auth.sessionHandler.revokeSession(sessionId)
        case _ => Future.successful()
      }

      onSuccess(future).flatMap {
        case _ => removeSessionCookie(auth.cookieName, auth.cookiePath)
      }
    }
  }

  def createSessionCookie(session: Session, cookieName: String, cookiePath: String = "/"): Directive0 = {
    setCookie(HttpCookie(cookieName, session.sessionId, expires = None, path = Some(cookiePath)))
  }

  def removeSessionCookie(cookieName: String, cookiePath: String = "/"): Directive0 = {
    deleteCookie(cookieName, path = cookiePath)
  }
}
