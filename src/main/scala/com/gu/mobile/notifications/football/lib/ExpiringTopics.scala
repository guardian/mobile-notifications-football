package com.gu.mobile.notifications.football.lib

import scala.concurrent.{ExecutionContext, Future}
import pa.PaClient
import Pa._
import Futures._
import com.gu.mobile.notifications.client.models.{Topic, TopicTypes}

trait ExpiringTopics {
  def getExpired(topics: List[Topic])(implicit context: ExecutionContext): Future[List[Topic]]
}

case class PaExpiringTopics(client: PaClient) extends ExpiringTopics {
  override def getExpired(topics: List[Topic])(implicit context: ExecutionContext): Future[List[Topic]] = {
    Future.sequenceSuccessful(topics.filter(_.`type` == TopicTypes.FootballMatch) map { topic =>
      client.matchInfo(topic.name) map { theMatch =>
        if (theMatch.hasEnded) Some(topic) else None
      }
    }) map { _.flatten }
  }
}