package com.gitgrid.managers

import com.gitgrid.models._
import reactivemongo.bson.BSONDateTime
import scala.concurrent.{ExecutionContext, Future}

class UserManager(db: Database)(implicit ec: ExecutionContext) {
  def createUser(userName: String, password: String): Future[User] = {
    for {
      u <- db.users.insert(User(userName = userName))
      p <- setPassword(u, password)
    } yield u
  }

  def setPassword(user: User, newPassword: String): Future[UserPassword] = {
    val now = BSONDateTime(System.currentTimeMillis)
    db.userPasswords.insert(UserPassword(userId = user.id.get, createdAt = now, hash = newPassword, hashAlgorithm = "plain"))
  }
}
