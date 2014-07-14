package com.gitgrid.models

import com.gitgrid._
import org.specs2.mutable._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes._
import reactivemongo.bson._
import scala.concurrent._

class TableSpec extends Specification with AsyncUtils {
  "Table" should {
    "properly CRUD on database" in new EmptyTestEnvironment {
      val coll = new TestEntityTable(db, db.mongoDbDatabase(db.collectionNamePrefix + "testentities"))
      val te1 = TestEntity(name = "te1")
      val te2 = TestEntity(name = "te2")
      val te3 = TestEntity(name = "te3")

      await(coll.all) must haveSize(0)
      await(coll.insert(te1))
      await(coll.all) must haveSize(1)
      await(coll.all)(0) === te1
      await(coll.insert(te2))
      await(coll.insert(te3))
      await(coll.all) must haveSize(3)
      await(coll.all).sortBy(u => u.name) === Seq(te1, te2, te3)
      await(coll.find(te2.id.get)) === Some(te2)
      await(coll.delete(te2))
      await(coll.all) must haveSize(2)
      await(coll.find(te2.id.get)) must beNone
      await(coll.update(te3.copy(name = "te3-new")))
      await(coll.find(te3.id.get)).get.name === "te3-new"

      ok
    }
  }
}

case class TestEntity(
  id: Option[BSONObjectID] = Some(BSONObjectID.generate),
  name: String = ""
) extends BaseModel

class TestEntityTable(database: Database, collection: BSONCollection)(implicit executor: ExecutionContext) extends Table[TestEntity](database, collection) {
  implicit val reader = TestEntityBSONFormat.Reader
  implicit val writer = TestEntityBSONFormat.Writer
}

object TestEntityBSONFormat {
  implicit object Reader extends BSONDocumentReader[TestEntity] {
    def read(doc: BSONDocument) = TestEntity(
      id = doc.getAs[BSONObjectID]("_id"),
      name = doc.getAs[String]("name").get
    )
  }

  implicit object Writer extends BSONDocumentWriter[TestEntity] {
    def write(obj: TestEntity): BSONDocument = BSONDocument(
      "_id" -> obj.id,
      "name" -> obj.name
    )
  }
}
