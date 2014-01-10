package com.gu.mobile.notifications.football

import com.amazonaws.services.sns.AmazonSNSClient
import com.gu.mobile.notifications.football.conf.GoalNotificationsConfig
import com.amazonaws.services.sns.model.PublishRequest
import scala.util.Try

object SNSQueue {
  private val client = {
    val client = new AmazonSNSClient(GoalNotificationsConfig.snsQueuePublish)
    client.setEndpoint("sns.eu-west-1.amazonaws.com")
    client
  }

  val topic = GoalNotificationsConfig.snsTopic

  def publish(subject: String, message: String) = {
    Try {
      client.publish(new PublishRequest().withSubject(subject).withMessage(message).withTopicArn(topic))
    }
  }
}
