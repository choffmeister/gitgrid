package com.gitgrid.models

import reactivemongo.bson._
import scala.concurrent.ExecutionContext

case class User(
  id: Option[BSONObjectID] = None,
  userName: String = ""
) extends BaseModel

class UserTable(database: Database)(implicit executor: ExecutionContext) extends Table[User](database, "users") {
  implicit val reader = UserBSONFormat.UserBSONReader
  implicit val writer = UserBSONFormat.UserBSONWriter

  def findByUserName(userName: String) = queryOne(BSONDocument("userName" -> userName))
}

object UserBSONFormat {
  implicit object UserBSONReader extends BSONDocumentReader[User] {
    def read(doc: BSONDocument) = User(
      id = doc.getAs[BSONObjectID]("_id"),
      userName = doc.getAs[String]("userName").get
    )
  }

  implicit object UserBSONWriter extends BSONDocumentWriter[User] {
    def write(obj: User): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "userName" -> obj.userName
    )
  }
}
