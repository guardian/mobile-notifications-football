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
      case "timeline" if matchEvent.matchTime.contains("0:00") => Some(KickOff)

      case "goal" => for {
        id <- matchEvent.id
        scorer <- matchEvent.players.headOption
        eventTime <- matchEvent.eventTime
        scoringTeam <- getTeam(scorer.teamID)
        otherTeam <- getOtherTeam(scorer.teamID)
      } yield Goal(id, scorer.name, scoringTeam, otherTeam, eventTime.toInt, matchEvent.addedTime.filterNot(_ == "0:00"))

      case "own goal" => for {
        id <- matchEvent.id
        scorer <- matchEvent.players.headOption
        eventTime <- matchEvent.eventTime
        scoringTeam <- getOtherTeam(scorer.teamID)
        otherTeam <- getTeam(scorer.teamID)
      } yield OwnGoal(id, scorer.name, scoringTeam, otherTeam, eventTime.toInt, matchEvent.addedTime.filterNot(_ == "0:00"))

      case "goal from penalty" => for {
        id <- matchEvent.id
        scorer <- matchEvent.players.headOption
        eventTime <- matchEvent.eventTime
        scoringTeam <- getTeam(scorer.teamID)
        otherTeam <- getOtherTeam(scorer.teamID)
      } yield PenaltyGoal(id, scorer.name, scoringTeam, otherTeam, eventTime.toInt, matchEvent.addedTime.filterNot(_ == "0:00"))

      case _ => None
    }
  }
}

object MatchEventTeam {
  def fromTeam(team: Team) = MatchEventTeam(team.id.toInt, team.name)
}

case class MatchEventTeam(id: Int, name: String)

sealed trait MatchEvent {
  def id: String
}
case object KickOff extends MatchEvent {
  override val id = "kickoff"
}

sealed trait ScoreEvent extends MatchEvent {
  val scorerName: String
  val scoringTeam: MatchEventTeam
  val otherTeam: MatchEventTeam
  val minute: Int
  /** A String because I'm not exactly sure what PA can give us here (their documentation is not exactly extensive).
    * TODO: At some point maybe convert to a Joda Time Period?
    */
  val addedTime: Option[String]
}

case class Goal(
  id: String,
  scorerName: String,
  scoringTeam: MatchEventTeam,
  otherTeam: MatchEventTeam,
  minute: Int,
  addedTime: Option[String]
) extends ScoreEvent

case class OwnGoal(
  id: String,
  scorerName: String,
  scoringTeam: MatchEventTeam,
  otherTeam: MatchEventTeam,
  minute: Int,
  addedTime: Option[String]
) extends ScoreEvent

case class PenaltyGoal(
  id: String,
  scorerName: String,
  scoringTeam: MatchEventTeam,
  otherTeam: MatchEventTeam,
  minute: Int,
  addedTime: Option[String]
) extends ScoreEvent

case object Result extends MatchEvent {
  override val id = "result"
}