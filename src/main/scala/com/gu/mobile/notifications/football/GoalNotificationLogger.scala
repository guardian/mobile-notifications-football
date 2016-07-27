package com.gu.mobile.notifications.football

import grizzled.slf4j.Logging
import com.gu.mobile.notifications.football.models._
import scala.concurrent.ExecutionContext.Implicits.global

trait GoalNotificationLogger extends Logging {
  val MaxHistoryLength: Int

  def logNotificationHistory(notificationResponse: NotificationHistoryItem) {
    Agents.notificationsHistory.sendOff(history => (notificationResponse :: history) take MaxHistoryLength)

    // TODO better logging ... really
    notificationResponse match {
      case NotificationSent(when, notification) => info(s"Sent notification at $when")
      case NotificationFailed(when, notification) => info(s"Failed to send notification at $when")
    }
  }
}
