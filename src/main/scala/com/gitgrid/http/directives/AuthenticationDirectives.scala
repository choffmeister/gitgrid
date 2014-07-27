package com.gitgrid.http.directives

import com.gitgrid.auth._
import com.gitgrid.models._
import spray.http.HttpHeaders.`WWW-Authenticate`
import scala.concurrent._
import shapeless.HNil
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.routing.Directives._
import spray.routing._
import spray.routing.authentication._
import spray.routing.AuthenticationFailedRejection._
import spray.routing.directives.AuthMagnet

trait AuthenticationDirectives {
  implicit val executor: ExecutionContext

  def authenticateOption[T](magnet: AuthMagnet[T]): Directive1[Option[T]] = authenticate(magnet)
    .flatMap(user => provide(Some(user)))
    .recoverPF {
      case AuthenticationFailedRejection(cause, ch) :: Nil if cause == CredentialsMissing => provide(Option.empty[T])
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

  def filterHttpChallenges(cond: HttpChallenge => Boolean): Directive0 = mapRejections { r =>
    r.map {
      case AuthenticationFailedRejection(c, ch) =>
        AuthenticationFailedRejection(c, ch.map {
          case `WWW-Authenticate`(ch) => `WWW-Authenticate`(ch.filter(cond))
          case x => x
        } filter {
          case `WWW-Authenticate`(Nil) => false
          case x => true
        })
      case r => r
    }
  }
}
