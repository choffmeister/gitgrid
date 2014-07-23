package com.gitgrid.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._
import scala.concurrent._

case class Session(
  id: BSONObjectID = BSONObjectID("00" * 12),
  userId: BSONObjectID,
  sessionId: String,
  expires: Option[BSONDateTime] = None
) extends BaseModel

class SessionTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[Session](database, collection) {
  implicit val reader = SessionBSONFormat.Reader
  implicit val writer = SessionBSONFormat.Writer

  override def preInsert(session: Session): Future[Session] = {
    val id = BSONObjectID.generate
    Future.successful(session.copy(id = id))
  }

  def findBySessionId(sessionId: String): Future[Option[Session]] = queryOne(BSONDocument("sessionId" -> sessionId))
  def deleteBySessionId(sessionId: String): Future[Unit] = collection.remove(BSONDocument("sessionId" -> sessionId)).map(_ => Unit)

  collection.indexesManager.ensure(Index(List("sessionId" -> IndexType.Ascending), unique = true))
}

object SessionBSONFormat {
  implicit object Reader extends BSONDocumentReader[Session] {
    def read(doc: BSONDocument) = Session(
      id = doc.getAs[BSONObjectID]("_id").get,
      sessionId = doc.getAs[String]("sessionId").get,
      userId = doc.getAs[BSONObjectID]("userId").get,
      expires = doc.getAs[BSONDateTime]("expires")
    )
  }

  implicit object Writer extends BSONDocumentWriter[Session] {
    def write(obj: Session): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "sessionId" -> obj.sessionId,
      "userId" -> obj.userId,
      "expires" -> obj.expires
    )
  }
}
