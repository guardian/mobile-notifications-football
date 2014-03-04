package com.gu.mobile.notifications.football.models

import pa.{MatchEvent => PaMatchEvent, Team, MatchEvents}

/** Match events we're interested in
  *
  * TODO: add red cards?
  *
  * TODO: add notifications for kick off, red cards, results
  */
object MatchEvent {
  def fromMatchEvents(matchEvents: MatchEvents): List[MatchEvent] = {
    val events = matchEvents.events.flatMap(fromMatchEvent(matchEvents) _)
    events ++ (if (matchEvents.isResult) List(Result) else Nil)
  }

  def fromMatchEvent(matchEvents: MatchEvents)(matchEvent: PaMatchEvent): Option[MatchEvent] = {
    val teams = List(matchEvents.homeTeam, matchEvents.awayTeam).map(MatchEventTeam.fromTeam)

    def getTeam(id: String) =
      teams.find(_.id == id.toInt)

    def getOtherTeam(id: String) =
      teams.filter(_.id != id.toInt) match {
        case team :: Nil => Some(team)
        case _ => None
      }

    matchEvent.eventType match {
      case "timeline" if matchEvent.matchTime == Some("0:00") => Some(KickOff)

      case "goal" => for {
        scorer <- matchEvent.players.headOption
        eventTime <- matchEvent.eventTime
        scoringTeam <- getTeam(scorer.teamID)
        otherTeam <- getOtherTeam(scorer.teamID)
      } yield Goal(scorer.name, scoringTeam, otherTeam, eventTime.toInt)

      case "own goal" => for {
        scorer <- matchEvent.players.headOption
        eventTime <- matchEvent.eventTime
        scoringTeam <- getOtherTeam(scorer.teamID)
        otherTeam <- getTeam(scorer.teamID)
      } yield OwnGoal(scorer.name, scoringTeam, otherTeam, eventTime.toInt)

      case _ => None
    }
  }
}

object MatchEventTeam {
  def fromTeam(team: Team) = MatchEventTeam(team.id.toInt, team.name)
}

case class MatchEventTeam(id: Int, name: String)

sealed trait MatchEvent
case object KickOff extends MatchEvent

sealed trait ScoreEvent extends MatchEvent {
  val scorerName: String
  val scoringTeam: MatchEventTeam
  val otherTeam: MatchEventTeam
  val minute: Int
}

case class Goal(
  scorerName: String,
  scoringTeam: MatchEventTeam,
  otherTeam: MatchEventTeam,
  minute: Int
) extends ScoreEvent

case class OwnGoal(
  scorerName: String,
  scoringTeam: MatchEventTeam,
  otherTeam: MatchEventTeam,
  minute: Int
) extends ScoreEvent

case object Result extends MatchEvent