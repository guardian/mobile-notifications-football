package com.gu.mobile.notifications.football.lib

import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.Notification
import com.gu.mobile.notifications.football.models._
import com.gu.mobile.notifications.client.models.IOSMessagePayload
import com.gu.mobile.notifications.client.models.Target
import com.gu.mobile.notifications.client.models.AndroidMessagePayload
import com.gu.mobile.notifications.football.models.OwnGoal
import scala.Some
import com.gu.mobile.notifications.client.models.MessagePayloads
import com.gu.mobile.notifications.client.models.Topic
import com.gu.mobile.notifications.client.models.Notification

object GoalNotificationBuilder {
  val FootballTeamTopicType = "football-team"
  val FootballMatchTopicType = "football-match"

  def uniqueIdentifier(goal: ScoreEvent, metadata: EventFeedMetadata) =
    s"goalAlert/${metadata.matchID}/${metadata.homeTeamScore}-${metadata.awayTeamScore}/" +
      s"${goal.scoringTeam.id}/${goal.minute}"

  def apply(goal: ScoreEvent, metadata: EventFeedMetadata): Notification = {
    Notification(
      `type` = "goal",
      uniqueIdentifier = uniqueIdentifier(goal, metadata),
      sender = "mobile-notifications-football",
      target = Target(
        regions = Set.empty,  /** Nothing ... is everything? :-O */
        topics = Set(
          Topic(
            FootballTeamTopicType,
            metadata.homeTeam.id.toString
          ),
          Topic(
            FootballTeamTopicType,
            metadata.awayTeam.id.toString
          ),
          Topic(
            FootballMatchTopicType,
            metadata.matchID
          ),
          /** Yes, the old apps registered by the team NAME. FFS **/
          Topic(
            FootballTeamTopicType,
            metadata.homeTeam.name
          ),
          Topic(
            FootballTeamTopicType,
            metadata.awayTeam.name
          )
        )
      ),
      timeToLiveInSeconds = (150 - goal.minute) * 60,
      payloads = MessagePayloads(
        android = Some(AndroidPayloadBuilder(goal, metadata)),
        ios = Some(IOSPayloadBuilder(goal, metadata))
      ),
      metadata = Map(
        "matchId" -> metadata.matchID,
        "homeTeamName" -> metadata.homeTeam.name,
        "homeTeamScore" -> metadata.homeTeamScore.toString,
        "awayTeamName" -> metadata.awayTeam.name,
        "awayTeamScore" -> metadata.awayTeamScore.toString,
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

  def apply(goal: ScoreEvent, metadata: EventFeedMetadata): AndroidMessagePayload = {
    AndroidMessagePayload(Map(
      Type -> GoalAlertType,
      AwayTeamName -> metadata.awayTeam.name,
      AwayTeamScore -> metadata.awayTeamScore.toString,
      HomeTeamName -> metadata.homeTeam.name,
      HomeTeamScore -> metadata.homeTeamScore.toString,
      ScoringTeamName -> goal.scoringTeam.name,
      ScorerName -> goal.scorerName,
      GoalMins -> goal.minute.toString,
      OtherTeamName -> goal.otherTeam.name,
      MatchId -> metadata.matchID,
      /** This flag exists for legacy reasons (the current PROD Android app will break if it's not here) */
      Debug -> "false"
    ))
  }
}

object IOSPayloadBuilder {
  val IOSMessageType = "t"
  val IOSGoalAlertType = "g"

  def apply(goal: ScoreEvent, metadata: EventFeedMetadata): IOSMessagePayload = {
    val goalTypeInfo = goal match {
      case OwnGoal(_, _, _, _, _) => Some("o.g.")
      case PenaltyGoal(_, _, _, _, _) => Some("pen")
      case _ => None
    }

    val extraInfo = List(goalTypeInfo, goal.addedTime.map("+" + _)).flatten match {
      case Nil => ""
      case xs => s" (${xs mkString " "})"
    }

    val message = s"${metadata.homeTeam.name} ${metadata.homeTeamScore}-" +
      s"${metadata.awayTeamScore} ${metadata.awayTeam.name}\n" +
      s"${goal.scorerName} ${goal.minute}min$extraInfo"

    IOSMessagePayload(message, Map(IOSMessageType -> IOSGoalAlertType))
  }
}