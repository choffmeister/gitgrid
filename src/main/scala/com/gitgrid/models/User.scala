package com.gitgrid.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._

import scala.concurrent._

case class User(
  id: BSONObjectID = BSONObjectID("00" * 12),
  userName: String,
  email: String,
  createdAt: BSONDateTime = BSONDateTime(0),
  updatedAt: BSONDateTime = BSONDateTime(0)
) extends BaseModel {
  require(userName.length > 3, "User names must be at least 3 characters long")
  require(email.matches("""^[a-zA-Z0-9_\-\.]+@[a-zA-Z0-9_\-\.]+\.[a-zA-Z]{2,}"""), "Email address must have valid format")
}

class UserTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[User](database, collection) {
  implicit val reader = UserBSONFormat.Reader
  implicit val writer = UserBSONFormat.Writer

  override def preInsert(user: User): Future[User] = {
    val id = BSONObjectID.generate
    val now = BSONDateTime(System.currentTimeMillis)
    Future.successful(user.copy(id = id, createdAt = now))
  }

  override def preUpdate(user: User): Future[User] = {
    val now = BSONDateTime(System.currentTimeMillis)
    Future.successful(user.copy(updatedAt = now))
  }

  def findByUserName(userName: String): Future[Option[User]] = queryOne(BSONDocument("userName" -> userName))

  collection.indexesManager.ensure(Index(List("userName" -> IndexType.Ascending), unique = true))
}

object UserBSONFormat {
  implicit object Reader extends BSONDocumentReader[User] {
    def read(doc: BSONDocument) = User(
      id = doc.getAs[BSONObjectID]("_id").get,
      userName = doc.getAs[String]("userName").get,
      email = doc.getAs[String]("email").get,
      createdAt = doc.getAs[BSONDateTime]("createdAt").get,
      updatedAt = doc.getAs[BSONDateTime]("updatedAt").get
    )
  }

  implicit object Writer extends BSONDocumentWriter[User] {
    def write(obj: User): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "userName" -> obj.userName,
      "email" -> obj.email,
      "createdAt" -> obj.createdAt,
      "updatedAt" -> obj.updatedAt
    )
  }
}
