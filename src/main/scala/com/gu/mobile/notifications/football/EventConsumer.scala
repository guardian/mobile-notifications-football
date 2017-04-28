package com.gu.mobile.notifications.football

import com.gu.mobile.notifications.client.ApiClient
import com.gu.mobile.notifications.football.models.{FootballMatchEvent, Goal, GoalContext, Score}
import com.gu.mobile.notifications.football.notificationbuilders.{GoalNotificationBuilder, MatchStatusNotificationBuilder}
import grizzled.slf4j.Logging
import pa.MatchDay

import scala.PartialFunction._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait EventConsumer {
  def receiveEvent(matchDay: MatchDay, previousEvents: List[pa.MatchEvent], event: pa.MatchEvent)(implicit ec: ExecutionContext): Future[Unit]
}

class EventConsumerImpl(
  goalNotificationBuilder: GoalNotificationBuilder,
  matchStatusNotificationBuilder: MatchStatusNotificationBuilder,
  notificationClient: ApiClient
) extends EventConsumer with Logging {

  override def receiveEvent(matchDay: MatchDay, previousEvents: List[pa.MatchEvent], event: pa.MatchEvent)(implicit ec: ExecutionContext): Future[Unit] = {
    FootballMatchEvent.fromPaMatchEvent(matchDay.homeTeam, matchDay.awayTeam)(event) map { ev =>
      sendAlerts(
        matchDay = matchDay,
        previousEvents = previousEvents.flatMap(FootballMatchEvent.fromPaMatchEvent(matchDay.homeTeam, matchDay.awayTeam)(_)),
        event = ev
      )
    } getOrElse Future.successful(())
  }

  private def sendAlerts(matchDay: MatchDay, previousEvents: List[FootballMatchEvent], event: FootballMatchEvent)(implicit ec: ExecutionContext): Future[Unit] = {
    val sentGoalAlert = condOpt(event) { case g: Goal => sendGoalAlert(matchDay, previousEvents)(g) }
    val sentMatchStatus = Some(sendMatchStatus(matchDay, previousEvents)(event))

    Future.sequence(List(sentGoalAlert, sentMatchStatus).flatten).map(_ => ())
  }

  private def sendGoalAlert(matchDay: MatchDay, previousEvents: List[FootballMatchEvent])(goal: Goal)(implicit ec: ExecutionContext): Future[Unit] = {
    logger.info(s"Sending goal alert $goal")

    val payload = goalNotificationBuilder.build(goal, matchDay, previousEvents)
    val result = notificationClient.send(payload)
    result.onComplete {
      case Success(Left(error)) => logger.error(s"Error sending $goal - ${error.description}")
      case Success(Right(())) => logger.info(s"Goal alert $goal successfully sent")
      case Failure(f) => logger.error(s"Error sending $goal ${f.getMessage}")
    }
    result
      .map(_ => ())
      .recover({ case _ => () })
  }

  private def sendMatchStatus(matchDay: MatchDay, previousEvents: List[FootballMatchEvent])(event: FootballMatchEvent)(implicit ec: ExecutionContext): Future[Unit] = {
    logger.info(s"Sending match status for even $event")

    val payload = matchStatusNotificationBuilder.build(event, matchDay, previousEvents)
    val result = notificationClient.send(payload)
    result.onComplete {
      case Success(Left(error)) => logger.error(s"Error sending match status for $event - ${error.description}")
      case Success(Right(())) => logger.info(s"Match status for $event successfully sent")
      case Failure(f) => logger.error(s"Error sending match status for $event ${f.getMessage}")
    }
    result
      .map(_ => ())
      .recover({ case _ => () })
  }
}
