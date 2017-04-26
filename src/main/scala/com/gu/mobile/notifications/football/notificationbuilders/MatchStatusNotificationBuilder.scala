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
      phase = matchInfo.matchStatus,
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
    case SecondHalf => "Kick-off!"
    case FullTime => "Full-Time"
    case _ => "The Guardian"
  }
}
