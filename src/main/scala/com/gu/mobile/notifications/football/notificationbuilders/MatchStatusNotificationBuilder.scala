package com.gu.mobile.notifications.football.notificationbuilders

import java.net.URI
import java.util.UUID

import com.gu.mobile.notifications.client.models.Importance.Major
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.football.models._
import pa.{MatchDay, MatchDayTeam}

import scala.PartialFunction.condOpt

class MatchStatusNotificationBuilder(mapiHost: String) {

  def build(triggeringEvent: FootballMatchEvent, matchInfo: MatchDay, previousEvents: List[FootballMatchEvent]): FootballMatchStatusPayload = {
    val topics = Set(
      Topic(TopicTypes.FootballTeam, matchInfo.homeTeam.id),
      Topic(TopicTypes.FootballTeam, matchInfo.awayTeam.id),
      Topic(TopicTypes.FootballMatch, matchInfo.id)
    )

    val allEvents = triggeringEvent :: previousEvents
    val goals = allEvents.collect { case g: Goal => g }
    val score = Score.fromGoals(matchInfo.homeTeam, matchInfo.awayTeam, goals)

    FootballMatchStatusPayload(
      title = eventTitle(triggeringEvent),
      message = mainMessage(matchInfo.homeTeam, matchInfo.awayTeam, score, matchInfo.matchStatus),
      sender = "mobile-notifications-football-lambda",
      awayTeamName = matchInfo.awayTeam.name,
      awayTeamScore = score.away,
      awayTeamMessage = teamMessage(matchInfo.awayTeam, allEvents),
      awayTeamId = matchInfo.awayTeam.id,
      homeTeamName = matchInfo.homeTeam.name,
      homeTeamScore = score.home,
      homeTeamMessage = teamMessage(matchInfo.homeTeam, allEvents),
      homeTeamId = matchInfo.homeTeam.id,
      matchId = matchInfo.id,
      competitionName = matchInfo.competition.map(_.name),
      venue = matchInfo.venue.map(_.name),
      mapiUrl = new URI(s"$mapiHost/sport/football/matches/${matchInfo.id}"),
      importance = Major,
      topic = topics,
      matchStatus = statuses.getOrElse(matchInfo.matchStatus, matchInfo.matchStatus),
      eventId = UUID.nameUUIDFromBytes(allEvents.toString.getBytes).toString,
      debug = false
    )
  }

  private def goalDescription(goal: Goal) = {
    val extraInfo = {
      val goalTypeInfo = condOpt(goal.goalType) {
        case OwnGoalType => "o.g."
        case PenaltyGoalType => "pen"
      }

      val addedTimeInfo = goal.addedTime.map("+" + _)

      List(goalTypeInfo, addedTimeInfo).flatten match {
        case Nil => ""
        case xs => s" ${xs.mkString(" ")}"
      }
    }

    s"""${goal.scorerName} ${goal.minute}'$extraInfo""".stripMargin
  }


  private def teamMessage(team: MatchDayTeam, events: List[FootballMatchEvent]) = {
    val msg = events.collect {
      case g: Goal if g.scoringTeam == team => goalDescription(g)
    }.mkString("\n")
    if (msg == "") " " else msg
  }

  private def mainMessage(homeTeam: MatchDayTeam, awayTeam: MatchDayTeam, score: Score, matchStatus: String) = {
    s"""${homeTeam.name} ${score.home}-${score.away} ${awayTeam.name}"""
  }

  private def eventTitle(fme: FootballMatchEvent): String = fme match {
    case _: Goal => "Goal!"
    case HalfTime => "Half-time"
    case KickOff => "Kick-off!"
    case SecondHalf => "Second-half start"
    case FullTime => "Full-Time"
    case _ => "The Guardian"
  }

  private val statuses = Map(
    ("KO", "1st"), // The Match has started (Kicked Off).

    ("HT", "HT"), // The Referee has blown the whistle for Half Time.

    ("SHS", "2nd"), // The Second Half of the Match has Started.

    ("FT", "FT"), // The Referee has blown the whistle for Full Time.
    ("PTFT", "FT"), // Penalty ShooT Full Time.
    ("Result", "FT"), // The Result is official.
    ("ETFT", "FT"), // Extra Time, Full Time has been blown.
    ("MC", "FT"), // Match has been Completed.

    ("FTET", "ET"), // Full Time, Extra Time it to be played.
    ("ETS", "ET"), // Extra Time has Started.
    ("ETHT", "ET"), // Extra Time Half Time has been called.
    ("ETSHS", "ET"), // Extra Time, Second Half has Started.

    ("FTPT", "PT"), // Full Time, Penalties are To be played.
    ("PT", "PT"), // Penalty ShooT Out has started.
    ("ETFTPT", "PT"), // Extra Time, Full Time, Penalties are To be played.

    ("Suspended", "S"), // Match has been Suspended.

    // don't really expect to see these (the way we handle data)
    ("Resumed", "R"), // Match has been Resumed.
    ("Abandoned", "A"), // Match has been Abandoned.
    ("Fixture", "F"), // Created Fixture is available and had been Created by us.
    ("-", "F"), // this sneaky one is not in the docs
    ("New", "N"), // Match A New Match has been added to our data.
    ("Cancelled", "C") // A Match has been Cancelled.
  )
}
