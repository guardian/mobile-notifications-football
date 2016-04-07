package com.gu.mobile.notifications.football.lib


import java.net.URI

import com.gu.mobile.notifications.client.models.Importance.Major
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.football.models._
import com.gu.mobile.notifications.football.conf.GoalNotificationsConfig

object GoalNotificationBuilder {

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

    val  topics = Set(
      Topic(
        TopicTypes.FootballTeam,
        metadata.homeTeam.id.toString
      ),
      Topic(
        TopicTypes.FootballTeam,
        metadata.awayTeam.id.toString
      ),
      Topic(
        TopicTypes.FootballMatch,
        metadata.matchID
      ),
      //The old apps registered by the team NAME
      Topic(
        TopicTypes.FootballTeam,
        metadata.homeTeam.name
      ),
      Topic(
        TopicTypes.FootballTeam,
        metadata.awayTeam.name
      )
    )
    GoalAlertPayload(
      sender = "mobile-notifications-football",
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
      mapiUrl = new URI(s"${GoalNotificationsConfig.mapiFootballHost}/match-info/${metadata.matchID}"),
      debug = false, // This flag exists for legacy reasons (the current PROD Android app will break if it's not here)
      addedTime = goal.addedTime,
      goalType = goalType,
      importance = Major,
      topic = topics
    )
  }
}