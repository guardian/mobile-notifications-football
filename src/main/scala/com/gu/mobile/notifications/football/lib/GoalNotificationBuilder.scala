package com.gu.mobile.notifications.football.lib

import scalaz._
import Scalaz._
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.football.lib.Pa.Goal
import pa.MatchDay
import com.gu.mobile.notifications.client.models.Notification

object GoalNotificationBuilder {
  val FootballTeamTopicType = "football-team"
  val FootballMatchTopicType = "football-match"

  def uniqueIdentifier(goal: Goal, matchDay: MatchDay) =
    s"goalAlert/${matchDay.id}/${matchDay.homeTeam.score.getOrElse(0)}-${matchDay.awayTeam.score.getOrElse(0)}/" +
      s"${goal.scoringTeam.id}/${goal.minute}"

  def apply(goal: Goal, matchDay: MatchDay): Notification = {
    Notification(
      `type` = "goal",
      uniqueIdentifier = uniqueIdentifier(goal, matchDay),
      sender = "mobile-notifications-football",
      target = Target(
        regions = Set.empty,  /** Nothing ... is everything? :-O */
        topics = Set(
          Topic(
            FootballTeamTopicType,
            matchDay.homeTeam.id
          ),
          Topic(
            FootballTeamTopicType,
            matchDay.awayTeam.id
          ),
          Topic(
            FootballMatchTopicType,
            matchDay.id
          )
        )
      ),
      timeToLiveInSeconds = (150 - goal.minute) * 60,
      payloads = MessagePayloads(
        android = Some(AndroidPayloadBuilder(goal, matchDay)),
        ios = Some(IOSPayloadBuilder(goal, matchDay))
      ),
      metadata = Map(
        "matchId" -> matchDay.id,
        "homeTeamName" -> matchDay.homeTeam.name,
        "homeTeamScore" -> matchDay.homeTeam.score.foldMap(_.toString),
        "awayTeamName" -> matchDay.awayTeam.name,
        "awayTeamScore" -> matchDay.awayTeam.score.foldMap(_.toString),
        "scorer" -> goal.scorerName,
        "minute" -> goal.minute.toString
      )
    )
  }
}

object AndroidPayloadBuilder {
  val Type = "type"
  val GoalAlertType = "goalAlert"
  val ScoringTeamName = "SCORING_TEAM_NAME"
  val OtherTeamName = "OTHER_TEAM_NAME"
  val ScorerName = "SCORER_NAME"
  val GoalMins = "GOAL_MINS"
  val HomeTeamName = "HOME_TEAM_NAME"
  val AwayTeamName = "AWAY_TEAM_NAME"
  val HomeTeamScore = "HOME_TEAM_SCORE"
  val AwayTeamScore = "AWAY_TEAM_SCORE"
  val MatchId = "matchId"
  val Debug = "debug"

  def apply(goal: Goal, matchDay: MatchDay): AndroidMessagePayload = {
    AndroidMessagePayload(Map(
      Type -> GoalAlertType,
      AwayTeamName -> matchDay.awayTeam.name,
      AwayTeamScore -> matchDay.awayTeam.score.foldMap(_.toString),
      HomeTeamName -> matchDay.homeTeam.name,
      HomeTeamScore -> matchDay.homeTeam.score.foldMap(_.toString),
      ScoringTeamName -> goal.scoringTeam.name,
      ScorerName -> goal.scorerName,
      GoalMins -> goal.minute.toString,
      OtherTeamName -> (Set(matchDay.homeTeam, matchDay.awayTeam) - goal.scoringTeam).head.name,
      MatchId -> matchDay.id,
      /** This flag exists for legacy reasons (the current PROD Android app will break if it's not here) */
      Debug -> "false"
    ))
  }
}

object IOSPayloadBuilder {
  val IOSMessageType = "t"
  val IOSGoalAlertType = "g"

  def apply(goal: Goal, matchDay: MatchDay): IOSMessagePayload = {
    val score = s"${matchDay.homeTeam.name} - ${matchDay.homeTeam.score.getOrElse(0)}\n" +
      s"${matchDay.awayTeam.name} - ${matchDay.awayTeam.score.getOrElse(0)}"

    val message = s"${goal.scorerName} scored for ${goal.scoringTeam.name}" +
      s" (${goal.minute} min)\n$score"

    IOSMessagePayload(message, Map(IOSMessageType -> IOSGoalAlertType))
  }
}