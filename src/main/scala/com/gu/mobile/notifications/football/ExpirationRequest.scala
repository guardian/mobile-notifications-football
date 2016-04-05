package com.gu.mobile.notifications.football

import com.gu.mobile.notifications.client.models.TopicTypes._
import com.gu.mobile.notifications.client.models.{Topic, TopicType}
import spray.json._
import spray.httpx.SprayJsonSupport

object ExpirationJsonImplicits extends DefaultJsonProtocol with SprayJsonSupport with GoalAndMetadataImplicits {
  implicit object topicTypeFormat extends JsonFormat[TopicType] {
    def write(obj: TopicType): JsValue = JsString(obj.toString)

    def read(json: JsValue): TopicType = json match {
      case JsString(Breaking.toString) => Breaking
      case JsString(Content.toString) => Content
      case JsString(TagContributor.toString) => TagContributor
      case JsString(TagKeyword.toString) => TagKeyword
      case JsString(TagSeries.toString) => TagSeries
      case JsString((TagBlog.toString)) => TagBlog
      case JsString((FootballMatch.toString)) => FootballMatch
      case JsString((FootballTeam.toString)) => FootballTeam
      case JsString((User.toString)) => User
      case JsString((Newsstand.toString)) => Newsstand
    }
  }

  implicit val topicFormat = jsonFormat2(Topic.apply)
  implicit val expirationRequestFormat = jsonFormat1(ExpirationRequest)
  implicit val expirationResponseFormat = jsonFormat1(ExpirationResponse)
}

case class ExpirationRequest(topics: List[Topic])
case class ExpirationResponse(topics: List[Topic])
