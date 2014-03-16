package com.gitgrid.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._
import scala.concurrent._

case class User(
  id: Option[BSONObjectID] = Some(BSONObjectID.generate),
  userName: String = ""
) extends BaseModel

class UserTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[User](database, collection) {
  implicit val reader = UserBSONFormat.UserBSONReader
  implicit val writer = UserBSONFormat.UserBSONWriter

  def findByUserName(userName: String): Future[Option[User]] = queryOne(BSONDocument("userName" -> userName))

  collection.indexesManager.ensure(Index(List("userName" -> IndexType.Ascending), unique = true))
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
