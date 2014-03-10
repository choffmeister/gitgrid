package com.gitgrid.models

import reactivemongo.bson._
import scala.concurrent.ExecutionContext

case class Session(
  id: Option[BSONObjectID],
  userId: BSONObjectID,
  sessionId: String
) extends BaseModel

class SessionTable(database: Database)(implicit executor: ExecutionContext) extends Table[Session](database, "sessions") {
  implicit val reader = SessionBSONFormat.SessionBSONReader
  implicit val writer = SessionBSONFormat.SessionBSONWriter
}

object SessionBSONFormat {
  implicit object SessionBSONReader extends BSONDocumentReader[Session] {
    def read(doc: BSONDocument) = Session(
      id = doc.getAs[BSONObjectID]("_id"),
      sessionId = doc.getAs[String]("sessionId").get,
      userId = doc.getAs[BSONObjectID]("userId").get
    )
  }

  implicit object SessionBSONWriter extends BSONDocumentWriter[Session] {
    def write(obj: Session): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "sessionId" -> obj.sessionId,
      "userId" -> obj.userId
    )
  }
}