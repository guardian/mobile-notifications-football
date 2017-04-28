package com.gu.football.models

import com.gu.football.MatchEventWithId
import com.gu.mobile.notifications.client.models.{DefaultGoalType, GoalType, OwnGoalType, PenaltyGoalType}
import pa.MatchDayTeam

import scala.PartialFunction._
import scala.util.Try

sealed trait MatchEvent
object MatchEvent {
  def fromEvent(homeTeam: MatchDayTeam, awayTeam: MatchDayTeam, event: MatchEventWithId): Option[MatchEvent] =
    KickOff.fromEvent(event) orElse Goal.fromEvent(homeTeam, awayTeam)(event)
}

case object KickOff extends MatchEvent {
  def fromEvent(event: MatchEventWithId): Option[MatchEvent] = {
    Option(event.eventType).filter(_ == "timeline" && event.matchTime.contains("0:00")).map(_ => KickOff)
  }
}

object Score {
  def fromGoals(homeTeam: MatchDayTeam, awayTeam: MatchDayTeam, goals: List[Goal]) = {
    val home = goals.count(_.scoringTeam == homeTeam)
    val away = goals.count(_.scoringTeam == awayTeam)

    Score(home, away)
  }
}

case class Score(home: Int, away: Int)

case class Goal(
  goalType: GoalType,
  scorerName: String,
  scoringTeam: MatchDayTeam,
  otherTeam: MatchDayTeam,
  minute: Int,
  addedTime: Option[String]
) extends MatchEvent

object Goal {

  def fromEvent(homeTeam: MatchDayTeam, awayTeam: MatchDayTeam)(event: MatchEventWithId): Option[Goal] = for {
    goalType <- goalTypeFromString(event.eventType)
    scorer <- event.players.headOption
    eventTime <- event.eventTime
    eventMinute <- Try(eventTime.toInt).toOption
    awayTeamScorer = scorer.teamID == awayTeam.id
    ownGoal = goalType == OwnGoalType
    teams = (homeTeam, awayTeam)
    (scoringTeam, otherTeam) = if (awayTeamScorer ^ ownGoal) teams.swap else teams
  } yield Goal(
      goalType,
      scorer.name,
      scoringTeam,
      otherTeam,
      eventMinute,
      event.addedTime.filterNot(_ == "0:00")
  )

  private def goalTypeFromString(s: String) = condOpt(s) {
    case "goal" => DefaultGoalType
    case "own goal" => OwnGoalType
    case "goal from penalty" => PenaltyGoalType
  }
}

case class GoalContext(
  home: MatchDayTeam,
  away: MatchDayTeam,
  matchId: String,
  score: Score
)