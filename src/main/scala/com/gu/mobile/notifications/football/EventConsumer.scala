package com.gu.mobile.notifications.football

import com.gu.Logging
import com.gu.mobile.notifications.client.models.NotificationPayload
import com.gu.mobile.notifications.football.models.{FootballMatchEvent, Goal}
import com.gu.mobile.notifications.football.notificationbuilders.{GoalNotificationBuilder, MatchStatusNotificationBuilder}
import pa.MatchDay

import scala.PartialFunction._
import scala.concurrent.ExecutionContext

class EventConsumer(
  goalNotificationBuilder: GoalNotificationBuilder,
  matchStatusNotificationBuilder: MatchStatusNotificationBuilder
) extends Logging {

  def receiveEvent(matchDay: MatchDay, previousEvents: List[pa.MatchEvent], event: pa.MatchEvent)(implicit ec: ExecutionContext): List[NotificationPayload] = {
    FootballMatchEvent.fromPaMatchEvent(matchDay.homeTeam, matchDay.awayTeam)(event) map { ev =>
      prepareNotifications(
        matchDay = matchDay,
        previousEvents = previousEvents.flatMap(FootballMatchEvent.fromPaMatchEvent(matchDay.homeTeam, matchDay.awayTeam)(_)),
        event = ev
      )
    } getOrElse Nil
  }

  private def prepareNotifications(matchDay: MatchDay, previousEvents: List[FootballMatchEvent], event: FootballMatchEvent)(implicit ec: ExecutionContext): List[NotificationPayload] = {
    val sentGoalAlert = condOpt(event) { case goal: Goal => goalNotificationBuilder.build(goal, matchDay, previousEvents) }
    val sentMatchStatus = Some(matchStatusNotificationBuilder.build(event, matchDay, previousEvents))

    val notifications = List(sentGoalAlert, sentMatchStatus).flatten
    logger.info(s"prepared the following notifications for match ${matchDay.id}, event $event: $notifications")

    notifications
  }
}
