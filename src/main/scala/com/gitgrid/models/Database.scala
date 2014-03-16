package com.gitgrid.models

import com.gitgrid.Config
import reactivemongo.api._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import scala.concurrent._

abstract class BaseModel {
  val id: Option[BSONObjectID]
}

abstract class Table[M <: BaseModel](database: Database, collection: BSONCollection)(implicit val executor: ExecutionContext) {
  implicit val reader: BSONDocumentReader[M]
  implicit val writer: BSONDocumentWriter[M]

  def all: Future[List[M]] = query(BSONDocument.empty)
  def find(id: BSONObjectID): Future[Option[M]] = queryOne(byId(id))
  def query(q: BSONDocument): Future[List[M]] = collection.find(q).cursor[M].collect[List]()
  def queryOne(q: BSONDocument): Future[Option[M]] = collection.find(q).one[M]
  def insert(m: M): Future[M] = collection.insert(m).map(_ => m)
  def update(m: M): Future[M] = collection.update(byId(m), m).map(_ => m)
  def delete(id: BSONObjectID): Future[Unit] = collection.remove(byId(id)).map(_ => Unit)
  def delete(m: M): Future[Unit] = delete(m.id.get)

  private def byId(id: BSONObjectID): BSONDocument = BSONDocument("_id" -> id)
  private def byId(id: Option[BSONObjectID]): BSONDocument = byId(id.get)
  private def byId(e: M): BSONDocument = byId(e.id.get)
}

class Database(connection: MongoConnection, databaseName: String, collectionNamePrefix: String = "")(implicit ec: ExecutionContext) {
  private val database = connection(databaseName)

  lazy val users = new UserTable(this, database(collectionNamePrefix + "users"))
  lazy val userPasswords = new UserPasswordTable(this, database(collectionNamePrefix + "user-passwords"))
  lazy val sessions = new SessionTable(this, database(collectionNamePrefix + "sessions"))
  lazy val projects = new ProjectTable(this, database(collectionNamePrefix + "projects"))
}

object Database {
  lazy val driver = new MongoDriver()

  def apply()(implicit ec: ExecutionContext): Database = {
    val connection = driver.connection(Config.mongoDbServers)
    new Database(connection, Config.mongoDbDatabaseName)
  }
}
