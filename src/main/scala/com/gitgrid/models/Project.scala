package com.gitgrid.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._
import scala.concurrent.ExecutionContext

case class Project(
  id: Option[BSONObjectID] = Some(BSONObjectID.generate),
  userId: BSONObjectID,
  canonicalName: String = ""
) extends BaseModel

class ProjectTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[Project](database, collection) {
  implicit val reader = ProjectBSONFormat.ProjectBSONReader
  implicit val writer = ProjectBSONFormat.ProjectBSONWriter

  collection.indexesManager.ensure(Index(List("userId" -> IndexType.Ascending, "canonicalName" -> IndexType.Ascending), unique = true))
}

object ProjectBSONFormat {
  implicit object ProjectBSONReader extends BSONDocumentReader[Project] {
    def read(doc: BSONDocument) = Project(
      id = doc.getAs[BSONObjectID]("_id"),
      userId = doc.getAs[BSONObjectID]("userId").get,
      canonicalName = doc.getAs[String]("canonicalName").get
    )
  }

  implicit object ProjectBSONWriter extends BSONDocumentWriter[Project] {
    def write(obj: Project): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "userId" -> obj.userId,
      "canonicalName" -> obj.canonicalName
    )
  }
}
