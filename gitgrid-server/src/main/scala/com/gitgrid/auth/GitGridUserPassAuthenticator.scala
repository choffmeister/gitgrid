package com.gitgrid.auth

import akka.pattern.after
import com.gitgrid._
import com.gitgrid.managers.UserManager
import com.gitgrid.models._
import com.gitgrid.utils.SimpleScheduler
import spray.routing.authentication._

import scala.concurrent._
import scala.util._

class GitGridUserPassAuthenticator(httpConf: HttpConfig, um: UserManager)(implicit ec: ExecutionContext) extends UserPassAuthenticator[User] {
  def apply(userPass: Option[UserPass]): Future[Option[User]] = {
    val auth = userPass match {
      case Some(UserPass(userName, password)) =>
        um.authenticateUser(userName, password)
      case None =>
        Future.successful(Option.empty[User])
    }
    val delay = after[Option[User]](httpConf.authPasswordValidationDelay, SimpleScheduler.instance)(future(None))
    val delayedAuth = Future.sequence(auth :: delay :: Nil).map(_(0))

    val promise = Promise[Option[User]]()
    auth.onSuccess {
      case Some(userPass) => promise.success(Some(userPass))
      case None => delayedAuth.onComplete {
        case Success(s) => promise.success(s)
        case Failure(f) => promise.failure(f)
      }
    }
    promise.future
  }
}
