package com.gu.mobile.notifications.football.lib

import org.scalatest.{ShouldMatchers, WordSpec}
import com.gu.mobile.notifications.football.lib.Pa.Goal
import com.gu.mobile.notifications.football.helpers.EmptyInstances
import pa.{MatchDayTeam, MatchDay}
import com.gu.mobile.notifications.client.models.IOSMessagePayload

class IOSPayloadBuilderSpec extends WordSpec with ShouldMatchers with EmptyInstances {
  "IOSPayloadBuilder" should {
    "build an appropriate message" in {
      val homeTeamFixture = MatchDayTeam.empty.copy(
        name = "Manchester United",
        score = Some(2),
        scorers = Some("Wayne Rooney (12), David Beckham (34)")
      )

      val awayTeamFixture = MatchDayTeam.empty.copy(
        name = "Bolton Wanderers",
        score = Some(1),
        scorers = Some("Joe Riley (30)")
      )

      IOSPayloadBuilder.apply(Goal(
        34,
        "David Beckham",
        homeTeamFixture
      ), MatchDay.empty.copy(
        homeTeam = homeTeamFixture,
        awayTeam = awayTeamFixture
      )) should equal(IOSMessagePayload("Manchester United 2-1 Bolton Wanderers\nDavid Beckham 34min", Map("t" -> "g")))
    }
  }
}
