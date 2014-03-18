package com.gitgrid.http

import com.gitgrid.git._
import com.gitgrid.http.routes._
import com.gitgrid.models._
import java.util.Date
import reactivemongo.bson._
import spray.httpx._
import spray.json._
import spray.routing.authentication.UserPass

trait JsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit object DateFormat extends JsonFormat[Date] {
    def write(date: Date) = JsNumber(date.getTime)
    def read(value: JsValue) =
      value match {
        case JsNumber(dateTicks) => new Date(dateTicks.toLong)
        case _ => deserializationError(s"Date time ticks expected. Got '$value'")
      }
  }

  implicit object BSONObjectIDFormat extends JsonFormat[BSONObjectID] {
    def write(id: BSONObjectID) = JsString(id.stringify)
    def read(value: JsValue) =
      value match {
        case JsString(str) => BSONObjectID(str)
        case _ => deserializationError("BSON ID expected: " + value)
      }
  }

  implicit object BSONDateTimeFormat extends JsonFormat[BSONDateTime] {
    def write(dateTime: BSONDateTime) = JsNumber(dateTime.value)
    def read(value: JsValue) =
      value match {
        case JsNumber(dateTimeTicks) => BSONDateTime(dateTimeTicks.toLong)
        case _ => deserializationError(s"Date time ticks expected. Got '$value'")
      }
  }

  implicit val userFormat = jsonFormat2(User)
  implicit val projectFormat = jsonFormat3(Project)

  implicit val userPassFormat = jsonFormat2(UserPass)
  implicit val authenticationResponseFormat = jsonFormat2(AuthenticationResponse)

  implicit val gitRefFormat = jsonFormat2(GitRef)
  implicit object GitObjectTypeFormat extends JsonFormat[GitObjectType] {
    def write(t: GitObjectType) =
      t match {
        case GitCommitObjectType => JsString("commit")
        case GitTreeObjectType => JsString("tree")
        case GitBlobObjectType => JsString("blob")
        case GitTagObjectType => JsString("tag")
        case _ => deserializationError("Unknown git object type")
      }

    def read(value: JsValue) = deserializationError("Deserialization of GitObjectType is not implemented")
  }
  implicit val gitCommitSignatureFormat = jsonFormat4(GitCommitSignature)
  implicit val gitCommitFormat = jsonFormat7(GitCommit)
  implicit val gitTreeEntryFormat = jsonFormat4(GitTreeEntry)
  implicit val gitTreeFormat = jsonFormat2(GitTree)
  implicit val gitBlobFormat = jsonFormat1(GitBlob)
}
