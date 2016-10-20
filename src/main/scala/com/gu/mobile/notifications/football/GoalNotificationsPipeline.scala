package com.gu.mobile.notifications.football

import scala.concurrent.duration._
import com.gu.mobile.notifications.football.models.{PenaltyGoal, EventFeedMetadata, OwnGoal, Goal}
import scala.concurrent.ExecutionContext.Implicits.global

object GoalNotificationsPipeline extends GoalNotificationsPipeline {
  val MaxHistoryLength: Int = 100
  val retrySendNotifications: Int = 5
  val UpdateInterval = 5.minutes
}

trait GoalNotificationsPipeline extends Streams with SNSQueue with GoalNotificationLogger {
  def start(): Unit = {
    val goalEventStream = getGoalEvents(getMatchDayStream)
    val notificationStream = getGoalEventsAsNotifications(goalEventStream)
    val notificationSendHistory = getNotificationResponses(notificationStream)

    notificationSendHistory.subscribe(logNotificationHistory _)

    goalEventStream.subscribe({ pair =>
      val (event, EventFeedMetadata(matchID, homeTeam, homeScore, awayTeam, awayScore)) = pair

      event match {
        case Goal(_, scorerName, scoringTeam, otherTeam, minute, _) =>
          logger.info(s"$matchID >>> $scorerName scored for ${scoringTeam.name} at $minute min: " +
            s"${homeTeam.name} $homeScore - $awayScore ${awayTeam.name}")
        case OwnGoal(_, scorerName, scoringTeam, otherTeam, minute, _) =>
          logger.info(s"$matchID >>> $scorerName (${otherTeam.name}}) scored an own goal at $minute min! " +
            s"${homeTeam.name} $homeScore - $awayScore ${awayTeam.name}")
        case PenaltyGoal(_, scorerName, scoringTeam, otherTeam, minute, _) =>
          logger.info(s"$matchID >>> $scorerName scored a penalty for ${scoringTeam.name} at $minute min: " +
            s"${homeTeam.name} $homeScore - $awayScore ${awayTeam.name}")
      }
    })

    notificationStream.subscribe(publishNotifications _)
  }
}
