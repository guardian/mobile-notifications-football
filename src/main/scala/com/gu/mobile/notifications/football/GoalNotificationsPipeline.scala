package com.gu.mobile.notifications.football

import scala.concurrent.duration._
import scala.util.{Failure, Success}
import com.gu.mobile.notifications.client.models.Notification

object GoalNotificationsPipeline extends GoalNotificationsPipeline {
  val MaxHistoryLength: Int = 100
  val retrySendNotifications: Int = 5
  val UpdateInterval = 3.seconds
}


trait GoalNotificationsPipeline extends Streams with SNSQueue with GoalNotificationLogger {

  def start(): Unit = {
    val matchDayStream = getMatchDayStream()
    val goalEventStream = getGoalEvents(matchDayStream)
    val notificationStream = getGoalEventsAsNotifications(goalEventStream)
    val notificationSendHistory = getNotificationResponses(notificationStream)

    notificationSendHistory subscribe logNotificationHistory
    goalEventStream subscribe logGoalEvents
    matchDayStream subscribe logLastMatchDay
    notificationStream subscribe publishNotifications
  }


  def publishNotifications: (Notification) => Unit = {
    notification =>
      publish("Goal notification created", notification.toString) match {
        case Success(publishResult) =>
          logger.info(s"Successfully sent notification to SNS queue: ${publishResult.getMessageId}")
        case Failure(error) =>
          logger.error(s"Encountered error when trying to add notification to SNS queue", error)
      }
  }
}
