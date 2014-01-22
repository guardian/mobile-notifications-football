package com.gu.mobile.notifications.football

import grizzled.slf4j.Logging
import com.gu.mobile.notifications.football.lib._
import pa.MatchDay
import scala.Some
import com.gu.mobile.notifications.football.models.{NotificationFailed, NotificationSent, NotificationHistoryItem}
import com.gu.mobile.notifications.football.lib.Pa._
import scala.concurrent.ExecutionContext.Implicits.global

trait GoalNotificationLogger extends Logging {
  val MaxHistoryLength: Int

  def logLastMatchDay: (List[MatchDay]) => Unit = {
    matchDays =>
      info(s"Got new set of match days: ${matchDays.map(_.summaryString).mkString(", ")}")
      Agents.lastMatchDaysSeen send const(Some(matchDays))
  }

  def logGoalEvents: (GoalEvent) => Unit = {
    goalEvent =>
      logger.info(s"Goal event: ${goalEvent.goal.scorerName} scored for ${goalEvent.goal.scoringTeam.name} at " +
        s"minute ${goalEvent.goal.minute}")
  }

  def logNotificationHistory: (NotificationHistoryItem) => Unit = {
    notificationResponse =>
      Agents.notificationsHistory.sendOff(history => (notificationResponse :: history) take MaxHistoryLength)

      // TODO better logging ... really
      notificationResponse match {
        case NotificationSent(when, notification, _) => info(s"Sent notification at $when")
        case NotificationFailed(when, notification) => info(s"Failed to send notification at $when")
      }
  }
}
