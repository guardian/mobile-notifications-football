package com.gu.mobile.notifications.football.lib

import com.gu.Logging
import com.gu.mobile.notifications.client.models.NotificationPayload
import com.gu.mobile.notifications.football.models.{FootballMatchEvent, Goal, MatchData}
import com.gu.mobile.notifications.football.notificationbuilders.{GoalNotificationBuilder, MatchStatusNotificationBuilder}
import pa.{MatchDay, MatchEvent}

import scala.PartialFunction._

class EventConsumer(
  goalNotificationBuilder: GoalNotificationBuilder,
  matchStatusNotificationBuilder: MatchStatusNotificationBuilder
) extends Logging {

  def receiveEvents(matchData: MatchData): List[NotificationPayload] = {
    matchData.eventsToProcess.flatMap { event =>
      receiveEvent(matchData.matchDay, matchData.allEvents, event)
    }
  }

  def receiveEvent(matchDay: MatchDay, events: List[MatchEvent], event: MatchEvent): List[NotificationPayload] = {
    logger.info(s"Processing event $event for match ${matchDay.id}")
    val previousEvents = events.takeWhile(_ != event)
    FootballMatchEvent.fromPaMatchEvent(matchDay.homeTeam, matchDay.awayTeam)(event) map { ev =>
      prepareNotifications(
        matchDay = matchDay,
        previousEvents = previousEvents.flatMap(FootballMatchEvent.fromPaMatchEvent(matchDay.homeTeam, matchDay.awayTeam)(_)),
        event = ev
      )
    } getOrElse Nil
  }

  private def prepareNotifications(matchDay: MatchDay, previousEvents: List[FootballMatchEvent], event: FootballMatchEvent): List[NotificationPayload] = {
    val sentGoalAlert = condOpt(event) { case goal: Goal => goalNotificationBuilder.build(goal, matchDay, previousEvents) }
    val sentMatchStatus = Some(matchStatusNotificationBuilder.build(event, matchDay, previousEvents))

    val notifications = List(sentGoalAlert, sentMatchStatus).flatten
    logger.info(s"prepared the following notifications for match ${matchDay.id}, event $event: $notifications")

    notifications
  }
}
