package com.gitgrid.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._
import scala.concurrent._

case class UserPassword(
  id: BSONObjectID = BSONObjectID("00" * 12),
  userId: BSONObjectID,
  hash: String = "",
  hashSalt: String = "",
  hashAlgorithm: String = "",
  createdAt: BSONDateTime = BSONDateTime(0)
) extends BaseModel

class UserPasswordTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[UserPassword](database, collection) {
  implicit val reader = UserPasswordBSONFormat.Reader
  implicit val writer = UserPasswordBSONFormat.Writer

  override def insert(userPassword: UserPassword): Future[UserPassword] = {
    val id = BSONObjectID.generate
    val now = BSONDateTime(System.currentTimeMillis)
    super.insert(userPassword.copy(id = id, createdAt = now))
  }

  def findCurrentPassword(userId: BSONObjectID): Future[Option[UserPassword]] = queryOne(BSONDocument(
    "$query" -> BSONDocument("userId" -> userId),
    "$orderby" -> BSONDocument("createdAt" -> -1)
  ))

  collection.indexesManager.ensure(Index(List("userId" -> IndexType.Ascending, "createdAt" -> IndexType.Descending)))
}

object UserPasswordBSONFormat {
  implicit object Reader extends BSONDocumentReader[UserPassword] {
    def read(doc: BSONDocument) = UserPassword(
      id = doc.getAs[BSONObjectID]("_id").get,
      userId = doc.getAs[BSONObjectID]("userId").get,
      hash = doc.getAs[String]("hash").get,
      hashSalt = doc.getAs[String]("hashSalt").get,
      hashAlgorithm = doc.getAs[String]("hashAlgorithm").get,
      createdAt = doc.getAs[BSONDateTime]("createdAt").get
    )
  }

  implicit object Writer extends BSONDocumentWriter[UserPassword] {
    def write(obj: UserPassword): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "userId" -> obj.userId,
      "hash" -> obj.hash,
      "hashSalt" -> obj.hashSalt,
      "hashAlgorithm" -> obj.hashAlgorithm,
      "createdAt" -> obj.createdAt
    )
  }
}
