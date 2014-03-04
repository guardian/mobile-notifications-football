package com.gu.mobile.notifications.football.lib

import org.scalatest.{ShouldMatchers, WordSpec}
import com.gu.mobile.notifications.client.models.IOSMessagePayload
import com.gu.mobile.notifications.football.models.{OwnGoal, MatchEventTeam, EventFeedMetadata, Goal}

class IOSPayloadBuilderSpec extends WordSpec with ShouldMatchers {
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

  "IOSPayloadBuilder" should {
    "build an appropriate message for a goal event" in {
      val eventFixture = Goal("David Beckham", manchesterUnited, boltonWanderers, 34)

      IOSPayloadBuilder.apply(eventFixture, metadataFixture) should equal(
        IOSMessagePayload("Manchester United 2-1 Bolton Wanderers\nDavid Beckham 34min", Map("t" -> "g"))
      )
    }

    "build an appropriate message for an own goal event" in {
      val eventFixture = OwnGoal("David Beckham", manchesterUnited, boltonWanderers, 34)

      IOSPayloadBuilder.apply(eventFixture, metadataFixture) should equal(
        IOSMessagePayload("Manchester United 2-1 Bolton Wanderers\nDavid Beckham 34min", Map("t" -> "g"))
      )
    }
  }
}
