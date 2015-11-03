package com.gu.mobile.notifications.football.lib

import com.gu.mobile.notifications.client.models.{DefaultGoalType, PenaltyGoalType, OwnGoalType, GoalAlertPayload}
import com.gu.mobile.notifications.client.models.NotificationTypes.GoalAlert
import com.gu.mobile.notifications.football.models._
import com.gu.mobile.notifications.client.models.legacy._
import com.gu.mobile.notifications.football.conf.GoalNotificationsConfig

object GoalNotificationBuilder {
  val FootballTeamTopicType = "football-team"
  val FootballMatchTopicType = "football-match"

  def uniqueIdentifier(goal: ScoreEvent, metadata: EventFeedMetadata) =
    s"goalAlert/${metadata.matchID}/${metadata.homeTeamScore}-${metadata.awayTeamScore}/" +
      s"${goal.scoringTeam.id}/${goal.minute}"

  def apply(goal: ScoreEvent, metadata: EventFeedMetadata): Notification = {
    val genericPayload = GenericPayload(goal, metadata)
    Notification(
      `type` = GoalAlert,
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
        android = Some(AndroidPayloadBuilder.fromGoalAlertPayload(genericPayload)),
        ios = Some(IOSPayloadBuilder.fromGoalAlertPayload(genericPayload))
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

object GenericPayload {

  def apply(goal: ScoreEvent, metadata: EventFeedMetadata): GoalAlertPayload = {
    val goalType = goal match {
      case OwnGoal(_, _, _, _, _) => OwnGoalType
      case PenaltyGoal(_, _, _, _, _) => PenaltyGoalType
      case _ => DefaultGoalType
    }

    val goalTypeInfo = goalType match {
      case OwnGoalType => Some("o.g.")
      case PenaltyGoalType => Some("pen")
      case _ => None
    }

    val extraInfo = List(goalTypeInfo, goal.addedTime.map("+" + _)).flatten match {
      case Nil => ""
      case xs => s" (${xs mkString " "})"
    }

    val message = s"${metadata.homeTeam.name} ${metadata.homeTeamScore}-" +
      s"${metadata.awayTeamScore} ${metadata.awayTeam.name}\n" +
      s"${goal.scorerName} ${goal.minute}min$extraInfo"

    GoalAlertPayload(
      notificationType = "goalAlert",
      title = "The Guardian",
      message = message,
      awayTeamName = metadata.awayTeam.name,
      awayTeamScore = metadata.awayTeamScore,
      homeTeamName = metadata.homeTeam.name,
      homeTeamScore = metadata.homeTeamScore,
      scoringTeamName = goal.scoringTeam.name,
      scorerName = goal.scorerName,
      goalMins = goal.minute,
      otherTeamName = goal.otherTeam.name,
      matchId = metadata.matchID,
      mapiUrl = s"${GoalNotificationsConfig.mapiFootballHost}/match-info/${metadata.matchID}",
      debug = false, // This flag exists for legacy reasons (the current PROD Android app will break if it's not here)
      addedTime = goal.addedTime,
      goalType = goalType
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
  val MapiUrl = "mapiUrl"

  def fromGoalAlertPayload(generic: GoalAlertPayload): AndroidMessagePayload = {
    AndroidMessagePayload(Map(
      Type -> generic.notificationType,
      AwayTeamName -> generic.awayTeamName,
      AwayTeamScore -> generic.awayTeamScore.toString,
      HomeTeamName -> generic.homeTeamName,
      HomeTeamScore -> generic.homeTeamScore.toString,
      ScoringTeamName -> generic.scoringTeamName,
      ScorerName -> generic.scorerName,
      GoalMins -> generic.goalMins.toString,
      OtherTeamName -> generic.otherTeamName,
      MatchId -> generic.matchId,
      MapiUrl -> generic.mapiUrl,
      Debug -> generic.debug.toString
    ))
  }
}

object IOSPayloadBuilder {
  val IOSMessageType = "t"
  val IOSGoalAlertType = "g"

  def fromGoalAlertPayload(generic: GoalAlertPayload): IOSMessagePayload = {
    IOSMessagePayload(generic.message, Map(IOSMessageType -> IOSGoalAlertType))
  }
}