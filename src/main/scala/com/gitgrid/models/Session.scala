package com.gitgrid.models

import reactivemongo.api.indexes._
import reactivemongo.bson._
import scala.concurrent._

case class Session(
  id: Option[BSONObjectID] = Some(BSONObjectID.generate),
  userId: BSONObjectID,
  sessionId: String,
  expires: Option[BSONDateTime] = None
) extends BaseModel

class SessionTable(database: Database, collectionName: String)(implicit executor: ExecutionContext) extends Table[Session](database, collectionName) {
  implicit val reader = SessionBSONFormat.SessionBSONReader
  implicit val writer = SessionBSONFormat.SessionBSONWriter

  def findBySessionId(sessionId: String): Future[Option[Session]] = queryOne(BSONDocument("sessionId" -> sessionId))
  def deleteBySessionId(sessionId: String): Future[Unit] = collection.remove(BSONDocument("sessionId" -> sessionId)).map(_ => Unit)

  collection.indexesManager.ensure(Index(List("sessionId" -> IndexType.Ascending), unique = true))
}

object SessionBSONFormat {
  implicit object SessionBSONReader extends BSONDocumentReader[Session] {
    def read(doc: BSONDocument) = Session(
      id = doc.getAs[BSONObjectID]("_id"),
      sessionId = doc.getAs[String]("sessionId").get,
      userId = doc.getAs[BSONObjectID]("userId").get,
      expires = doc.getAs[BSONDateTime]("expires")
    )
  }

  implicit object SessionBSONWriter extends BSONDocumentWriter[Session] {
    def write(obj: Session): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "sessionId" -> obj.sessionId,
      "userId" -> obj.userId,
      "expires" -> obj.expires
    )
  }
}
