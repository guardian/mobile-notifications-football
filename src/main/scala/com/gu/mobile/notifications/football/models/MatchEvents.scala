package com.gu.mobile.notifications.football.models

import com.gu.mobile.notifications.client.models.{DefaultGoalType, GoalType, OwnGoalType, PenaltyGoalType}

import scala.PartialFunction._
import scala.util.Try

sealed trait FootballMatchEvent
object FootballMatchEvent {
  def fromPaMatchEvent(homeTeam: pa.MatchDayTeam, awayTeam: pa.MatchDayTeam)(event: pa.MatchEvent): Option[FootballMatchEvent] =
    MatchPhaseEvent.fromEvent(event) orElse Goal.fromEvent(homeTeam, awayTeam)(event)
}

object Score {
  def fromGoals(homeTeam: pa.MatchDayTeam, awayTeam: pa.MatchDayTeam, goals: List[Goal]) = {
    val home = goals.count(_.scoringTeam == homeTeam)
    val away = goals.count(_.scoringTeam == awayTeam)

    Score(home, away)
  }
}

case class Score(home: Int, away: Int)

case class Goal(
  goalType: GoalType,
  scorerName: String,
  scoringTeam: pa.MatchDayTeam,
  otherTeam: pa.MatchDayTeam,
  minute: Int,
  addedTime: Option[String]
) extends FootballMatchEvent

object Goal {

  def fromEvent(homeTeam: pa.MatchDayTeam, awayTeam: pa.MatchDayTeam)(event: pa.MatchEvent): Option[Goal] = for {
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


trait MatchPhaseEvent extends FootballMatchEvent

object MatchPhaseEvent {
  def fromEvent(event: pa.MatchEvent): Option[MatchPhaseEvent] =
    condOpt(event.eventType) {
      case "timeline" if event.matchTime.contains("0:00") => KickOff
      case "full-time" => FullTime
      case "half-time" => HalfTime
      case "second-half" => SecondHalf
  }
}

case object KickOff extends MatchPhaseEvent
case object FullTime extends MatchPhaseEvent
case object HalfTime extends MatchPhaseEvent
case object SecondHalf extends MatchPhaseEvent

case class GoalContext(
  home: pa.MatchDayTeam,
  away: pa.MatchDayTeam,
  matchId: String,
  score: Score
)