package com.gu.mobile.notifications.football.models

import pa.{MatchEvent => PaMatchEvent, MatchEvents}

/** Match events we're interested in
  *
  * TODO: add red cards?
  *
  * TODO: add notifications for kick off, red cards, results
  */
object MatchEvent {
  def fromMatchEvents(matchEvents: MatchEvents): List[MatchEvent] = {
    val events = matchEvents.events.flatMap(fromMatchEvent)
    events ++ (if (matchEvents.isResult) List(Result) else Nil)
  }

  def fromMatchEvent(matchEvent: PaMatchEvent): Option[MatchEvent] = {
    matchEvent.eventType match {
      case "timeline" if matchEvent.matchTime == Some("0:00") => Some(KickOff)

      case "goal" => for {
        scorer <- matchEvent.players.headOption
        eventTime <- matchEvent.eventTime
      } yield Goal(scorer.name, scorer.teamID.toInt, eventTime.toInt)

      case "own goal" => for {
        scorer <- matchEvent.players.headOption
        eventTime <- matchEvent.eventTime
      } yield OwnGoal(scorer.name, scorer.teamID.toInt, eventTime.toInt)

      case _ => None
    }
  }
}

sealed trait MatchEvent

case object KickOff extends MatchEvent
case class Goal(scorerName: String, teamId: Int, minute: Int) extends MatchEvent
case class OwnGoal(scorerName: String, teamId: Int, minute: Int) extends MatchEvent
case object Result extends MatchEvent