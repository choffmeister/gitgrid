package com.gitgrid.auth

import com.gitgrid.managers.UserManager
import com.gitgrid.models._
import spray.routing.authentication._

import scala.concurrent._

class GitGridUserPassAuthenticator(um: UserManager)(implicit ec: ExecutionContext) extends UserPassAuthenticator[User] {
  def apply(userPass: Option[UserPass]): Future[Option[User]] = userPass match {
    case Some(UserPass(userName, password)) =>
      um.authenticateUser(userName, password)
    case None =>
      Future.successful(Option.empty[User])
  }
}
