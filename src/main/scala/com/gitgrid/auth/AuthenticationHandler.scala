package com.gitgrid.auth

import com.gitgrid.Config
import scala.concurrent.{ExecutionContext, Future}
import com.gitgrid.models.{Database, User}

class AuthenticationHandler(implicit config: Config, ec: ExecutionContext) {
  val db = Database()

  def authenticate(userName: String, password: String): Future[Option[User]] = {
    db.users.findByUserName(userName).flatMap {
      case Some(u) =>
        db.userPasswords.findCurrentPassword(u.id.get).map {
          case Some(pwd) =>
            pwd.hashAlgorithm match {
              case "plain" =>
                if (pwd.hash == password) Some(u) else None
              case _ =>
                throw new Exception(s"Unsupported hash algorithm ${pwd.hashAlgorithm}")
            }
          case _ =>
            None
        }
      case _ =>
        Future.successful(None)
    }
  }
}
