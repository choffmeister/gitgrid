package com.gitgrid.models

import reactivemongo.bson._
import scala.concurrent.ExecutionContext

case class UserPassword(
  id: Option[BSONObjectID] = None,
  userId: BSONObjectID,
  createdAt: BSONDateTime,
  hash: String = "",
  hashSalt: String = "",
  hashAlgorithm: String = ""
) extends BaseModel

class UserPasswordTable(database: Database)(implicit executor: ExecutionContext) extends Table[UserPassword](database, "user-passwords") {
  implicit val reader = UserPasswordBSONFormat.UserPasswordBSONReader
  implicit val writer = UserPasswordBSONFormat.UserPasswordBSONWriter

  def findCurrentPassword(userId: BSONObjectID) = queryOne(BSONDocument(
    "$query" -> BSONDocument(
      "userId" -> userId
    ),
    "$orderby" -> BSONDocument(
      "createdAt" -> -1
    )
  ))
}

object UserPasswordBSONFormat {
  implicit object UserPasswordBSONReader extends BSONDocumentReader[UserPassword] {
    def read(doc: BSONDocument) = UserPassword(
      id = doc.getAs[BSONObjectID]("_id"),
      userId = doc.getAs[BSONObjectID]("userId").get,
      createdAt = doc.getAs[BSONDateTime]("createdAt").get,
      hash = doc.getAs[String]("hash").get,
      hashSalt = doc.getAs[String]("hashSalt").get,
      hashAlgorithm = doc.getAs[String]("hashAlgorithm").get
    )
  }

  implicit object UserPasswordBSONWriter extends BSONDocumentWriter[UserPassword] {
    def write(obj: UserPassword): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "userId" -> obj.userId,
      "createdAt" -> obj.createdAt,
      "hash" -> obj.hash,
      "hashSalt" -> obj.hashSalt,
      "hashAlgorithm" -> obj.hashAlgorithm
    )
  }
}
