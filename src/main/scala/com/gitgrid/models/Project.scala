package com.gitgrid.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._
import scala.concurrent._

case class Project(
  id: Option[BSONObjectID] = Some(BSONObjectID.generate),
  userId: BSONObjectID,
  name: String = ""
) extends BaseModel

class ProjectTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[Project](database, collection) {
  implicit val reader = ProjectBSONFormat.ProjectBSONReader
  implicit val writer = ProjectBSONFormat.ProjectBSONWriter

  def findByFullQualifiedName(ownerName: String, projectName: String): Future[Option[Project]] = {
    database.users.findByUserName(ownerName).flatMap {
      case Some(user) => queryOne(BSONDocument("userId" -> user.id.get, "name" -> projectName))
      case _ => future(None)
    }
  }

  collection.indexesManager.ensure(Index(List("userId" -> IndexType.Ascending, "name" -> IndexType.Ascending), unique = true))
}

object ProjectBSONFormat {
  implicit object ProjectBSONReader extends BSONDocumentReader[Project] {
    def read(doc: BSONDocument) = Project(
      id = doc.getAs[BSONObjectID]("_id"),
      userId = doc.getAs[BSONObjectID]("userId").get,
      name = doc.getAs[String]("name").get
    )
  }

  implicit object ProjectBSONWriter extends BSONDocumentWriter[Project] {
    def write(obj: Project): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "userId" -> obj.userId,
      "name" -> obj.name
    )
  }
}
