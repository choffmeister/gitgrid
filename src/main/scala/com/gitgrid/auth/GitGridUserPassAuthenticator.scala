package com.gitgrid.auth

import com.gitgrid.Config
import com.gitgrid.models._
import scala.Some
import scala.concurrent._
import spray.routing.authentication._

class GitGridUserPassAuthenticator(db: Database)(implicit ec: ExecutionContext) extends UserPassAuthenticator[User] {
  def apply(userPass: Option[UserPass]): Future[Option[User]] = userPass match {
    case Some(UserPass(userName, password)) =>
      db.users.findByUserName(userName).flatMap {
        case Some(user) =>
          checkPassword(user, password).map {
            case true => Some(user)
            case false => Option.empty[User]
          }
        case None =>
          Future.successful(Option.empty[User])
      }
    case None =>
      Future.successful(Option.empty[User])
  }

  def checkPassword(user: User, password: String): Future[Boolean] = {
    db.userPasswords.findCurrentPassword(user.id.get).map {
      case Some(pwd) => checkPassword(pwd.hash, pwd.hashSalt, pwd.hashAlgorithm, password)
      case _ => throw new Exception("Could not find current user password")
    }
  }

  def checkPassword(hash: String, hashSalt: String, hashAlgorithm: String, password: String): Boolean = hashAlgorithm match {
    case "plain" if hash == password => true
    case _ => false
  }
}

object GitGridUserPassAuthenticator {
  def apply()(implicit config: Config, ec: ExecutionContext): GitGridUserPassAuthenticator = new GitGridUserPassAuthenticator(Database())
}
