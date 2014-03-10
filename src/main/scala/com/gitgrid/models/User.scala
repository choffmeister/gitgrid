package com.gitgrid.models

import reactivemongo.bson._
import scala.concurrent.ExecutionContext

case class User(
  id: Option[BSONObjectID] = None,
  userName: String = "",
  passwordHash: String = "",
  passwordHashSalt: String = "",
  passwordHashAlgorithm: String = ""
) extends BaseModel

class UserTable(database: Database)(implicit executor: ExecutionContext) extends Table[User](database, "users") {
  implicit val reader = UserBSONFormat.UserBSONReader
  implicit val writer = UserBSONFormat.UserBSONWriter
}

object UserBSONFormat {
  implicit object UserBSONReader extends BSONDocumentReader[User] {
    def read(doc: BSONDocument) = User(
      id = doc.getAs[BSONObjectID]("_id"),
      userName = doc.getAs[String]("userName").get,
      passwordHash = doc.getAs[String]("passwordHash").get,
      passwordHashSalt = doc.getAs[String]("passwordHashSalt").get,
      passwordHashAlgorithm = doc.getAs[String]("passwordHashAlgorithm").get
    )
  }

  implicit object UserBSONWriter extends BSONDocumentWriter[User] {
    def write(obj: User): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "userName" -> obj.userName,
      "passwordHash" -> obj.passwordHash,
      "passwordHashSalt" -> obj.passwordHashSalt,
      "passwordHashAlgorithm" -> obj.passwordHashAlgorithm
    )
  }
}
