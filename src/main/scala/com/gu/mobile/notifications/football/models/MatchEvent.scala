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
    def getTeam(id: String) =
      List(matchEvents.homeTeam, matchEvents.awayTeam).find(_.id == id).map(MatchEventTeam.fromTeam)

    matchEvent.eventType match {
      case "timeline" if matchEvent.matchTime == Some("0:00") => Some(KickOff)

      case "goal" => for {
        scorer <- matchEvent.players.headOption
        eventTime <- matchEvent.eventTime
        team <- getTeam(scorer.teamID)
      } yield Goal(scorer.name, team, eventTime.toInt)

      case "own goal" => for {
        scorer <- matchEvent.players.headOption
        eventTime <- matchEvent.eventTime
        team <- getTeam(scorer.teamID)
      } yield OwnGoal(scorer.name, team, eventTime.toInt)

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
case class Goal(scorerName: String, team: MatchEventTeam, minute: Int) extends MatchEvent

/** NB: The team here is the team whose player scored the unfortunate goal, not the team who gets the point */
case class OwnGoal(scorerName: String, team: MatchEventTeam, minute: Int) extends MatchEvent
case object Result extends MatchEvent