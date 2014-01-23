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

    notificationSendHistory subscribe logNotificationHistory
    goalEventStream subscribe logGoalEvents
    matchDayStream subscribe logLastMatchDay
    notificationStream subscribe publishNotifications
  }
}
