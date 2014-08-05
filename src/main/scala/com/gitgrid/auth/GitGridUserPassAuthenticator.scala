package com.gitgrid.auth

import akka.pattern.after
import com.gitgrid.Config
import com.gitgrid.managers.UserManager
import com.gitgrid.models._
import com.gitgrid.utils.SimpleScheduler
import spray.routing.authentication._

import scala.concurrent._
import scala.util.{Success, Failure}

class GitGridUserPassAuthenticator(cfg: Config, um: UserManager)(implicit ec: ExecutionContext) extends UserPassAuthenticator[User] {
  def apply(userPass: Option[UserPass]): Future[Option[User]] = {
    val auth = userPass match {
      case Some(UserPass(userName, password)) =>
        um.authenticateUser(userName, password)
      case None =>
        Future.successful(Option.empty[User])
    }
    val delay = after[Option[User]](cfg.passwordsValidationDelay, SimpleScheduler.instance)(future(None))
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
