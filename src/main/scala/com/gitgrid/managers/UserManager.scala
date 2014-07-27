package com.gitgrid.managers

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import com.gitgrid.models._
import com.gitgrid.utils.NonceGenerator
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

  def changeUserPassword(user: User, newPassword: String, hashAlgorithm: String = "pbkdf2-sha1-10000-128"): Future[UserPassword] = {
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

  def generateSalt(hashAlgorithm: String): Seq[Byte] = hashAlgorithm match {
    case "plain" => Seq.empty[Byte]
    case "pbkdf2-sha1-10000-128" => NonceGenerator.generateBytes(128)
    case _ => throw new Exception(s"Unknown hash algorithm $hashAlgorithm")
  }

  def calculateHash(password: String, hashSalt: Seq[Byte], hashAlgorithm: String): Seq[Byte] = hashAlgorithm match {
    case "plain" => password.getBytes("UTF-8")
    case "pbkdf2-sha1-10000-128" =>
      val spec = new PBEKeySpec(password.toCharArray, hashSalt.toArray, 10000, 128)
      val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
      skf.generateSecret(spec).getEncoded().toSeq
    case _ => throw new Exception(s"Unknown hash algorithm $hashAlgorithm")
  }
}
