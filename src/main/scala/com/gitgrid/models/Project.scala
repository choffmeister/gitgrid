package com.gitgrid.models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._
import scala.concurrent._

case class Project(
  id: Option[BSONObjectID] = None,
  ownerId: BSONObjectID,
  name: String,
  description: String = "",
  public: Boolean = false,
  createdAt: BSONDateTime = BSONDateTime(0),
  updatedAt: BSONDateTime = BSONDateTime(0),
  pushedAt: Option[BSONDateTime] = None
) extends BaseModel

class ProjectTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[Project](database, collection) {
  implicit val reader = ProjectBSONFormat.Reader
  implicit val writer = ProjectBSONFormat.Writer

  override def insert(project: Project): Future[Project] = {
    val id = Some(BSONObjectID.generate)
    val now = BSONDateTime(System.currentTimeMillis)
    super.insert(project.copy(id = id, createdAt = now, updatedAt = now))
  }

  override def update(project: Project): Future[Project] = {
    val now = BSONDateTime(System.currentTimeMillis)
    super.insert(project.copy(updatedAt = now))
  }

  def findByFullQualifiedName(ownerName: String, projectName: String): Future[Option[Project]] = {
    database.users.findByUserName(ownerName).flatMap {
      case Some(user) => queryOne(BSONDocument("ownerId" -> user.id.get, "name" -> projectName))
      case _ => future(None)
    }
  }

  collection.indexesManager.ensure(Index(List("ownerId" -> IndexType.Ascending, "name" -> IndexType.Ascending), unique = true))
}

object ProjectBSONFormat {
  implicit object Reader extends BSONDocumentReader[Project] {
    def read(doc: BSONDocument) = Project(
      id = doc.getAs[BSONObjectID]("_id"),
      ownerId = doc.getAs[BSONObjectID]("ownerId").get,
      name = doc.getAs[String]("name").get,
      description = doc.getAs[String]("description").get,
      public = doc.getAs[Boolean]("public").get,
      createdAt = doc.getAs[BSONDateTime]("createdAt").get,
      updatedAt = doc.getAs[BSONDateTime]("updatedAt").get,
      pushedAt = doc.getAs[BSONDateTime]("pushedAt")
    )
  }

  implicit object Writer extends BSONDocumentWriter[Project] {
    def write(obj: Project): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "ownerId" -> obj.ownerId,
      "name" -> obj.name,
      "description" -> obj.description,
      "public" -> obj.public,
      "createdAt" -> obj.createdAt,
      "updatedAt" -> obj.updatedAt,
      "pushedAt" -> obj.pushedAt
    )
  }
}
