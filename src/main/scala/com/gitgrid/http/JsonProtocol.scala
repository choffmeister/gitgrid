package com.gitgrid.http

import java.util.Date

import com.gitgrid.git._
import com.gitgrid.http.routes._
import com.gitgrid.models._
import reactivemongo.bson._
import spray.httpx._
import spray.json._

trait DateJsonProtocol extends DefaultJsonProtocol {
  implicit object DateFormat extends JsonFormat[Date] {
    def write(date: Date) = JsNumber(date.getTime)
    def read(value: JsValue) =
      value match {
        case JsNumber(dateTicks) => new Date(dateTicks.toLong)
        case _ => deserializationError(s"Date time ticks expected. Got '$value'")
      }
  }
}

trait BSONJsonProtocol extends DefaultJsonProtocol {
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
}

trait GitJsonProtocol extends DefaultJsonProtocol with DateJsonProtocol {
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

    def read(value: JsValue) =
      value match {
        case JsString("commit") => GitCommitObjectType
        case JsString("tree") => GitTreeObjectType
        case JsString("blob") => GitBlobObjectType
        case JsString("tag") => GitTagObjectType
        case _ => deserializationError("Unknown git object type")
      }
  }
  implicit val gitCommitSignatureFormat = jsonFormat4(GitCommitSignature)
  implicit val gitCommitFormat = jsonFormat7(GitCommit)
  implicit val gitTreeEntryFormat = jsonFormat4(GitTreeEntry)
  implicit val gitTreeFormat = jsonFormat2(GitTree)
  implicit val gitBlobFormat = jsonFormat1(GitBlob)
}

trait JsonProtocol extends DefaultJsonProtocol
  with DateJsonProtocol
  with BSONJsonProtocol
  with GitJsonProtocol
  with SprayJsonSupport
{
  implicit val userFormat = jsonFormat4(User)
  implicit val projectFormat = jsonFormat9(Project)

  implicit val authenticationRequestFormat = jsonFormat3(AuthenticationRequest)
  implicit val authenticationResponseFormat = jsonFormat3(AuthenticationResponse)
  implicit val registrationRequestFormat = jsonFormat2(RegistrationRequest)
}
