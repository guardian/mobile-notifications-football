package com.gu.mobile.notifications.football

import com.gu.mobile.notifications.football.models.{MatchEventTeam, EventFeedMetadata, Goal}
import spray.json.DefaultJsonProtocol
import spray.httpx.SprayJsonSupport

trait GoalAndMetadataImplicits { self: DefaultJsonProtocol with SprayJsonSupport =>
  implicit val matchEventTeamFormat = jsonFormat2(MatchEventTeam.apply)
  implicit val goalFormat = jsonFormat6(Goal)
  implicit val metadataFormat = jsonFormat5(EventFeedMetadata.apply)
  implicit val goalAndMetadataFormat = jsonFormat2(GoalAndMetadata)
}

case class GoalAndMetadata(goal: Goal, metadata: EventFeedMetadata)