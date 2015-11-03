package com.gu.mobile.notifications.football.lib

import com.gu.mobile.notifications.client.models.legacy.Topic
import scala.concurrent.{ExecutionContext, Future}
import pa.PaClient
import Pa._
import Futures._

trait ExpiringTopics {
  def getExpired(topics: List[Topic])(implicit context: ExecutionContext): Future[List[Topic]]
}

case class PaExpiringTopics(client: PaClient) extends ExpiringTopics {
  override def getExpired(topics: List[Topic])(implicit context: ExecutionContext): Future[List[Topic]] = {
    Future.sequenceSuccessful(topics.filter(_.`type` == GoalNotificationBuilder.FootballMatchTopicType) map { topic =>
      client.matchInfo(topic.name) map { theMatch =>
        if (theMatch.hasEnded) Some(topic) else None
      }
    }) map { _.flatten }
  }
}