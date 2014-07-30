package com.gitgrid.managers

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

import com.gitgrid.Config
import com.gitgrid.models._
import com.gitgrid.utils.BinaryStringConverter._
import com.gitgrid.utils.NonceGenerator
import reactivemongo.bson._

import scala.concurrent.{ExecutionContext, Future}

class UserManager(cfg: Config, db: Database)(implicit ec: ExecutionContext) {
  def createUser(user: User, password: String): Future[User] = {
    for {
      u <- db.users.insert(user)
      p <- changeUserPassword(u, password, cfg.passwordsStorageDefaultAlgorithm)
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

  def changeUserPassword(user: User, newPassword: String, algorithm: String): Future[UserPassword] = {
    db.userPasswords.insert(UserPassword(
      userId = user.id,
      createdAt = BSONDateTime(System.currentTimeMillis),
      password = algorithm match {
        case `Plain`(None) => `Plain`(newPassword)
        case `PBKDF2-HMAC-SHA1`(iterations, keyLength, None, None) => `PBKDF2-HMAC-SHA1`(iterations, keyLength, newPassword, None)
        case _ => throw new Exception(s"Unknown hash algorithm $algorithm")
      }
    ))
  }

  def validateUserPassword(user: User, password: String): Future[Boolean] = {
    db.userPasswords.findCurrentPassword(user.id).map {
      case Some(pwd) => pwd.password match {
        case s@`Plain`(p) => `Plain`(password) == s
        case s@`PBKDF2-HMAC-SHA1`(it, kl, Some(ha), Some(sa)) => `PBKDF2-HMAC-SHA1`(it, kl, password, Some(sa)) == s
        case _ => throw new Exception("Current user password has a unknown format")
      }
      case _ => throw new Exception("Could not find current user password")
    }
  }
}

sealed abstract class PasswordHashAlgorithm

object `Plain` extends PasswordHashAlgorithm {
  val hashPattern = s"^plain:(.*)$$".r
  val algoPattern = s"^plain$$".r

  def apply(password: String): String = s"plain:$password"

  def unapply(hash: String): Option[(Option[String])] = hash match {
    case hashPattern(password) => Some((Some(password)))
    case algoPattern() => Some((None))
    case _ => None
  }
}

object `PBKDF2-HMAC-SHA1` extends PasswordHashAlgorithm {
  val b64 = "[a-zA-Z0-9\\+/=]+"
  val hashPattern = s"^pbkdf2:hmac-sha1:(\\d+):(\\d+):($b64):($b64)$$".r
  val algoPattern = s"^pbkdf2:hmac-sha1:(\\d+):(\\d+)$$".r

  def apply(iterations: Int, keyLength: Int, password: String, salt: Option[Seq[Byte]]): String = {
      val salt2 = salt match {
        case Some(salt) => salt.toArray
        case None => NonceGenerator.generateBytes(128)
      }
      val spec = new PBEKeySpec(password.toCharArray, salt2, iterations, keyLength)
      val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
      val hash = skf.generateSecret(spec).getEncoded()
      s"pbkdf2:hmac-sha1:$iterations:$keyLength:${bytesToBase64(hash)}:${bytesToBase64(salt2)}"
  }

  def unapply(hash: String): Option[(Int, Int, Option[Seq[Byte]], Option[Seq[Byte]])] = hash match {
    case hashPattern(iterations, keyLength, hash, salt) =>
      Some((iterations.toInt, keyLength.toInt, Some(base64ToBytes(hash).toSeq), Some(base64ToBytes(salt).toSeq)))
    case algoPattern(iterations, keyLength) =>
      Some((iterations.toInt, keyLength.toInt, None, None))
    case _ => None
  }
}
