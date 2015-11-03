package com.gu.mobile.notifications.football

import com.amazonaws.services.sns.AmazonSNSClient
import com.gu.mobile.notifications.football.conf.GoalNotificationsConfig
import com.amazonaws.services.sns.model.PublishRequest
import scala.util.{Failure, Success, Try}
import com.gu.mobile.notifications.client.models.legacy.Notification
import grizzled.slf4j.Logging

trait SNSQueue extends Logging {
  private val maybeClient = for {
    credentials <- GoalNotificationsConfig.snsQueuePublishCredentials
  } yield {
    val client = new AmazonSNSClient(credentials)
    client.setEndpoint("sns.eu-west-1.amazonaws.com")
    client
  }

  lazy private val maybeTopic = GoalNotificationsConfig.snsTopic

  def publishNotifications(notification: Notification) {
    (for {
      client <- maybeClient
      topic <- maybeTopic
    } yield Try {
      client.publish(new PublishRequest()
        .withSubject("Goal notification created")
        .withMessage(notification.toString)
        .withTopicArn(topic))
    } match {
      case Success(publishResult) =>
        logger.info(s"Successfully sent notification to SNS queue: ${publishResult.getMessageId}")
      case Failure(error) =>
        logger.error(s"Encountered error when trying to add notification to SNS queue", error)
    }) getOrElse Failure(new RuntimeException(
      "Could not publish to SNS queue - you must set topic and credentials in config"
    ))
  }
}
