package com.gitgrid.managers

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

import com.gitgrid.models._
import com.gitgrid.utils.BinaryStringConverter._
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

  def changeUserPassword(user: User, newPassword: String, algorithm: PasswordHashAlgorithm = `PBKDF2-HMAC-SHA1`): Future[UserPassword] = {
    db.userPasswords.insert(UserPassword(
      userId = user.id,
      createdAt = BSONDateTime(System.currentTimeMillis),
      password = algorithm match {
        case `Plain` => `Plain`(newPassword)
        case `PBKDF2-HMAC-SHA1` => `PBKDF2-HMAC-SHA1`(100000, 128, newPassword)
      }
    ))
  }

  def validateUserPassword(user: User, password: String): Future[Boolean] = {
    db.userPasswords.findCurrentPassword(user.id).map {
      case Some(pwd) => pwd.password match {
        case s@`Plain`(p) => `Plain`(password) == s
        case s@`PBKDF2-HMAC-SHA1`(it, kl, ha, sa) => `PBKDF2-HMAC-SHA1`(it, kl, password, Some(sa)) == s
        case _ => throw new Exception("Current user password has a unknown format")
      }
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

sealed abstract class PasswordHashAlgorithm

object `Plain` extends PasswordHashAlgorithm {
  val pattern = s"^plain:(.*)".r

  def apply(password: String): String = s"plain:$password"

  def unapply(hash: String): Option[(String)] = hash match {
    case pattern(password) => Some((password))
    case _ => None
  }
}

object `PBKDF2-HMAC-SHA1` extends PasswordHashAlgorithm {
  val b64 = "[a-zA-Z0-9\\+/=]+"
  val pattern = s"^pbkdf2:hmac-sha1:(\\d+):(\\d+):($b64):($b64)".r

  def apply(iterations: Int, keyLength: Int, password: String, salt: Option[Seq[Byte]] = None): String = {
      val salt2 = salt match {
        case Some(salt) => salt.toArray
        case None => NonceGenerator.generateBytes(128)
      }
      val spec = new PBEKeySpec(password.toCharArray, salt2, iterations, keyLength)
      val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
      val hash = skf.generateSecret(spec).getEncoded()
      s"pbkdf2:hmac-sha1:$iterations:$keyLength:${bytesToBase64(hash)}:${bytesToBase64(salt2)}"
  }

  def unapply(hash: String): Option[(Int, Int, Seq[Byte], Seq[Byte])] = hash match {
    case pattern(iterations, keyLength, hash, salt) =>
      Some((iterations.toInt, keyLength.toInt, base64ToBytes(hash).toSeq, base64ToBytes(salt).toSeq))
    case _ => None
  }
}
