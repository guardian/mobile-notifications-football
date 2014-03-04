package com.gu.mobile.notifications.football

import grizzled.slf4j.Logging
import com.gu.mobile.notifications.football.lib._
import pa.MatchDay
import scala.Some
import com.gu.mobile.notifications.football.models._
import com.gu.mobile.notifications.football.lib.Pa._
import scala.concurrent.ExecutionContext.Implicits.global

trait GoalNotificationLogger extends Logging {
  val MaxHistoryLength: Int

  def logLastMatchDay(matchDays: List[MatchDay]) {
    info(s"Got new set of match days: ${matchDays.map(_.summaryString).mkString(", ")}")
    Agents.lastMatchDaysSeen send const(Some(matchDays))
  }

  def logGoalEvents(goalEvent: ScoreEvent) {
    logger.info(s"Goal event: ${goalEvent.scorerName} scored for ${goalEvent.scoringTeam.name} at " +
      s"minute ${goalEvent.minute}")
  }

  def logNotificationHistory(notificationResponse: NotificationHistoryItem) {
    Agents.notificationsHistory.sendOff(history => (notificationResponse :: history) take MaxHistoryLength)

    // TODO better logging ... really
    notificationResponse match {
      case NotificationSent(when, notification, _) => info(s"Sent notification at $when")
      case NotificationFailed(when, notification) => info(s"Failed to send notification at $when")
    }
  }
}
