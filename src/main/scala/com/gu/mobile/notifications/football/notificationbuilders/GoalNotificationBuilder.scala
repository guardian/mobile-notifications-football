package com.gu.mobile.notifications.football.notificationbuilders

import java.net.URI

import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.Importance.{Major, _}
import com.gu.mobile.notifications.football.models.{FootballMatchEvent, Goal, GoalContext, Score}
import pa.{MatchDay, MatchDayTeam}

import PartialFunction.condOpt

class GoalNotificationBuilder(mapiHost: String) {

  private def alertMessage(goal: Goal, score: Score, homeTeam: String, awayTeam: String) = {
    val extraInfo = {
      val goalTypeInfo = condOpt(goal.goalType) {
        case OwnGoalType => "o.g."
        case PenaltyGoalType => "pen"
      }

      val addedTimeInfo = goal.addedTime.map("+" + _)

      List(goalTypeInfo, addedTimeInfo).flatten match {
        case Nil => ""
        case xs => s" (${xs.mkString(" ")})"
      }
    }

    s"""$homeTeam ${score.home}-${score.away} $awayTeam
       |${goal.scorerName} ${goal.minute}min$extraInfo""".stripMargin
  }

  def build(goal: Goal, matchDay: MatchDay, previousEvents: List[FootballMatchEvent]): GoalAlertPayload = {
    val goalsToDate = goal :: previousEvents.collect({ case g: Goal => g })
    val score = Score.fromGoals(matchDay.homeTeam, matchDay.awayTeam, goalsToDate)
    val goalContext = GoalContext(matchDay.homeTeam, matchDay.awayTeam, matchDay.id, score)

    val message = alertMessage(goal, goalContext.score, goalContext.home.name, goalContext.away.name)

    val topics = Set(
      Topic(TopicTypes.FootballTeam, goalContext.home.id),
      Topic(TopicTypes.FootballTeam, goalContext.away.id),
      Topic(TopicTypes.FootballMatch, goalContext.matchId)
    )

    GoalAlertPayload(
      sender = "mobile-notifications-football-lambda",
      title = "The Guardian",
      message = message,
      awayTeamName = goalContext.away.name,
      awayTeamScore = goalContext.score.away,
      homeTeamName = goalContext.home.name,
      homeTeamScore = goalContext.score.home,
      scoringTeamName = goal.scoringTeam.name,
      scorerName = goal.scorerName,
      goalMins = goal.minute,
      otherTeamName = goal.otherTeam.name,
      matchId = goalContext.matchId,
      mapiUrl = new URI(s"$mapiHost/sport/football/matches/${goalContext.matchId}"),
      debug = false,
      addedTime = goal.addedTime,
      goalType = goal.goalType,
      importance = Major,
      topic = topics
    )
  }
}
