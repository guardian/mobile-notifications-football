package com.gu.mobile.notifications.football

import spray.json._
import spray.httpx.SprayJsonSupport
import com.gu.mobile.notifications.client.models.Topic

object ExpirationJsonImplicits extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val topicFormat = jsonFormat2(Topic)
  implicit val expirationRequestFormat = jsonFormat1(ExpirationRequest)
  implicit val expirationResponseFormat = jsonFormat1(ExpirationResponse)
}

case class ExpirationRequest(topics: List[Topic])
case class ExpirationResponse(topics: List[Topic])