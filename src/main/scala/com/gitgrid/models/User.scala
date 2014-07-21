package com.gitgrid.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._
import scala.concurrent._

case class User(
  id: Option[BSONObjectID] = None,
  userName: String = "",
  createdAt: BSONDateTime = BSONDateTime(0),
  updatedAt: BSONDateTime = BSONDateTime(0)
) extends BaseModel

class UserTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[User](database, collection) {
  implicit val reader = UserBSONFormat.Reader
  implicit val writer = UserBSONFormat.Writer

  override def insert(user: User): Future[User] = {
    val id = Some(BSONObjectID.generate)
    val now = BSONDateTime(System.currentTimeMillis)
    super.insert(user.copy(id = id, createdAt = now, updatedAt = now))
  }

  override def update(user: User): Future[User] = {
    val now = BSONDateTime(System.currentTimeMillis)
    super.insert(user.copy(updatedAt = now))
  }

  def findByUserName(userName: String): Future[Option[User]] = queryOne(BSONDocument("userName" -> userName))

  collection.indexesManager.ensure(Index(List("userName" -> IndexType.Ascending), unique = true))
}

object UserBSONFormat {
  implicit object Reader extends BSONDocumentReader[User] {
    def read(doc: BSONDocument) = User(
      id = doc.getAs[BSONObjectID]("_id"),
      userName = doc.getAs[String]("userName").get,
      createdAt = doc.getAs[BSONDateTime]("createdAt").get,
      updatedAt = doc.getAs[BSONDateTime]("updatedAt").get
    )
  }

  implicit object Writer extends BSONDocumentWriter[User] {
    def write(obj: User): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "userName" -> obj.userName,
      "createdAt" -> obj.createdAt,
      "updatedAt" -> obj.updatedAt
    )
  }
}
