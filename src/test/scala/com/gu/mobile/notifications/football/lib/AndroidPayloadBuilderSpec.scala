package com.gu.mobile.notifications.football.lib

import com.gu.mobile.notifications.football.conf.GoalNotificationsConfig
import org.scalatest.{ShouldMatchers, WordSpec}
import com.gu.mobile.notifications.client.models.AndroidMessagePayload
import com.gu.mobile.notifications.football.models._
import com.gu.mobile.notifications.football.models.Goal

class AndroidPayloadBuilderSpec extends WordSpec with ShouldMatchers {
  val manchesterUnited = MatchEventTeam(
    34,
    "Manchester United"
  )

  val boltonWanderers = MatchEventTeam(
    41,
    "Bolton Wanderers"
  )

  val metadataFixture = EventFeedMetadata(
    "1234",
    manchesterUnited,
    2,
    boltonWanderers,
    1
  )

  "AndroidPayloadBuilder" should {
    "build an appropriate message for a goal event" in {
      val eventFixture = Goal("David Beckham", manchesterUnited, boltonWanderers, 34, None)

      AndroidPayloadBuilder(eventFixture, metadataFixture) should equal(
        AndroidMessagePayload(Map(
          "type" -> "goalAlert",
          "SCORING_TEAM_NAME" -> "Manchester United",
          "OTHER_TEAM_NAME" -> "Bolton Wanderers",
          "SCORER_NAME" -> "David Beckham",
          "GOAL_MINS" -> "34",
          "HOME_TEAM_NAME" ->  "Manchester United",
          "AWAY_TEAM_NAME" -> "Bolton Wanderers",
          "HOME_TEAM_SCORE" -> "2",
          "AWAY_TEAM_SCORE" -> "1",
          "matchId" -> "1234",
          "debug" -> "false",
          "mapiUrl" -> s"${GoalNotificationsConfig.mapiFootballHost}/match-info/1234"
        ))
      )
    }
  }
}
