package com.gitgrid.managers

import com.gitgrid.models._
import reactivemongo.bson._
import scala.concurrent.{ExecutionContext, Future}

class UserManager(db: Database)(implicit ec: ExecutionContext) {
  def createUser(user: User, password: String): Future[User] = {
    for {
      u <- db.users.insert(user)
      p <- changeUserPassword(u, password)
    } yield u
  }

  def authenticateUser(userName: String, password: String): Future[Option[User]] = {
    db.users.findByUserName(userName).flatMap {
      case Some(user) =>
        validateUserPassword(user, password).map {
          case true => Some(user)
          case false => Option.empty[User]
        }
      case None =>
        Future.successful(Option.empty[User])
    }
  }

  def changeUserPassword(user: User, newPassword: String, hashAlgorithm: String = "plain"): Future[UserPassword] = {
    val now = BSONDateTime(System.currentTimeMillis)
    val hashSalt = generateSalt(hashAlgorithm)
    val hash = calculateHash(newPassword, hashSalt, hashAlgorithm)

    db.userPasswords.insert(UserPassword(
      userId = user.id,
      createdAt = now,
      hash = hash,
      hashSalt = hashSalt,
      hashAlgorithm = hashAlgorithm
    ))
  }

  def validateUserPassword(user: User, password: String): Future[Boolean] = {
    db.userPasswords.findCurrentPassword(user.id).map {
      case Some(pwd) => pwd.hash == calculateHash(password, pwd.hashSalt, pwd.hashAlgorithm)
      case _ => throw new Exception("Could not find current user password")
    }
  }

  def generateSalt(hashAlgorithm: String): String = hashAlgorithm match {
    case "plain" => ""
    case _ => throw new Exception(s"Unknown hash algorithm $hashAlgorithm")
  }

  def calculateHash(password: String, hashSalt: String, hashAlgorithm: String): String = hashAlgorithm match {
    case "plain" => password
    case _ => throw new Exception(s"Unknown hash algorithm $hashAlgorithm")
  }
}
