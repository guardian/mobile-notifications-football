package com.gu.mobile.notifications.football

import scala.concurrent.duration._

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

    notificationSendHistory.subscribe(logNotificationHistory _)
    goalEventStream.subscribe(logGoalEvents _)
    matchDayStream.subscribe(logLastMatchDay _)
    notificationStream.subscribe(publishNotifications _)
  }
}
