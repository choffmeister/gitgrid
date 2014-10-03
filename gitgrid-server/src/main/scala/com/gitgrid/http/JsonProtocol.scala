package com.gitgrid.http

import java.util.Date

import com.gitgrid.WorkerProtocol.QueryResult
import com.gitgrid.auth._
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

trait AuthJsonProtocol extends DefaultJsonProtocol {
  implicit object OAuth2AccessTokenResponseFormat extends RootJsonFormat[OAuth2AccessTokenResponse] {
    def write(res: OAuth2AccessTokenResponse) = JsObject(
      "token_type" -> JsString(res.tokenType),
      "access_token" -> JsString(res.accessToken),
      "expires_in" -> JsNumber(res.expiresIn)
    )
    def read(value: JsValue) =
      value.asJsObject.getFields("token_type", "access_token", "expires_in") match {
        case Seq(JsString(tokenType), JsString(accessToken), JsNumber(expiresIn)) =>
          OAuth2AccessTokenResponse(tokenType, accessToken, expiresIn.toLong)
        case _ => throw new DeserializationException("OAuth2 token response expected")
      }
  }
}

trait WorkersJsonProtocol extends DefaultJsonProtocol with GitJsonProtocol {
  implicit object WorkerMasterQueryResultFormat extends RootJsonWriter[QueryResult] {
    override def write(obj: QueryResult): JsValue = {
      JsObject(Map(
        "queued" -> JsArray(obj.queued.map(wi => JsString(wi.toString))),
        "running" -> JsArray(obj.running.map(wi => JsString(wi.toString)))
      ))
    }
  }
}

trait JsonProtocol extends DefaultJsonProtocol
  with DateJsonProtocol
  with BSONJsonProtocol
  with AuthJsonProtocol
  with GitJsonProtocol
  with WorkersJsonProtocol
  with SprayJsonSupport
{
  implicit val userFormat = jsonFormat5(User)
  implicit val registrationRequestFormat = jsonFormat3(RegistrationRequest)
  implicit val projectFormat = jsonFormat9(Project)
}
