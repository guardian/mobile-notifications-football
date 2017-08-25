package com.gu.mobile.notifications.football.lib

import com.gu.Logging
import com.gu.mobile.notifications.client.models.NotificationPayload
import com.gu.mobile.notifications.football.models.{FootballMatchEvent, Goal, MatchDataWithArticle}
import com.gu.mobile.notifications.football.notificationbuilders.{GoalNotificationBuilder, MatchStatusNotificationBuilder}
import pa.{MatchDay, MatchEvent}

import scala.PartialFunction._

class EventConsumer(
  goalNotificationBuilder: GoalNotificationBuilder,
  matchStatusNotificationBuilder: MatchStatusNotificationBuilder
) extends Logging {

  def receiveEvents(matchData: MatchDataWithArticle): List[NotificationPayload] = {
    matchData.filteredEvents.flatMap { event =>
      receiveEvent(matchData.matchDay, matchData.allEvents, event, matchData.articleId)
    }
  }

  def receiveEvent(matchDay: MatchDay, events: List[MatchEvent], event: MatchEvent, articleId: Option[String]): List[NotificationPayload] = {
    logger.debug(s"Processing event $event for match ${matchDay.id}")
    val previousEvents = events.takeWhile(_ != event)
    FootballMatchEvent.fromPaMatchEvent(matchDay.homeTeam, matchDay.awayTeam)(event) map { ev =>
      prepareNotifications(
        matchDay = matchDay,
        previousEvents = previousEvents.flatMap(FootballMatchEvent.fromPaMatchEvent(matchDay.homeTeam, matchDay.awayTeam)(_)),
        event = ev,
        articleId
      )
    } getOrElse Nil
  }

  private def prepareNotifications(
    matchDay: MatchDay,
    previousEvents: List[FootballMatchEvent],
    event: FootballMatchEvent,
    articleId: Option[String]
  ): List[NotificationPayload] = {
    val sentGoalAlert = condOpt(event) { case goal: Goal => goalNotificationBuilder.build(goal, matchDay, previousEvents) }
    val sentMatchStatus = Some(matchStatusNotificationBuilder.build(event, matchDay, previousEvents, articleId))

    val notifications = List(sentGoalAlert, sentMatchStatus).flatten
    logger.info(s"Prepared the following notifications for match ${matchDay.id}, event $event: $notifications")

    notifications
  }
}
