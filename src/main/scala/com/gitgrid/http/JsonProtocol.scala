package com.gitgrid.http

import com.gitgrid.models._
import reactivemongo.bson._
import spray.httpx._
import spray.json._

trait JsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
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

  implicit val authenticationRequestFormat = jsonFormat2(AuthenticationRequest)
  implicit val authenticationResponseFormat = jsonFormat2(AuthenticationResponse)
  implicit val authenticationStateFormat = jsonFormat1(AuthenticationState)
}
