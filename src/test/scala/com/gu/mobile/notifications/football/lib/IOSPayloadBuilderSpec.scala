package com.gu.mobile.notifications.football.lib

import org.scalatest.{ShouldMatchers, WordSpec}
import com.gu.mobile.notifications.client.models.IOSMessagePayload
import com.gu.mobile.notifications.football.models._
import com.gu.mobile.notifications.football.models.Goal
import com.gu.mobile.notifications.client.models.IOSMessagePayload
import com.gu.mobile.notifications.football.models.OwnGoal

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
      val eventFixture = Goal("David Beckham", manchesterUnited, boltonWanderers, 34, None)

      IOSPayloadBuilder.apply(eventFixture, metadataFixture) should equal(
        IOSMessagePayload("Manchester United 2-1 Bolton Wanderers\nDavid Beckham 34min", Map("t" -> "g"))
      )
    }

    "build an appropriate message for an own goal event" in {
      val eventFixture = OwnGoal("David Beckham", manchesterUnited, boltonWanderers, 34, None)

      IOSPayloadBuilder.apply(eventFixture, metadataFixture) should equal(
        IOSMessagePayload("Manchester United 2-1 Bolton Wanderers\nDavid Beckham 34min (o.g.)", Map("t" -> "g"))
      )
    }

    "build an appropriate message for a penalty goal event" in {
      val eventFixture = PenaltyGoal("David Beckham", manchesterUnited, boltonWanderers, 82, None)

      IOSPayloadBuilder.apply(eventFixture, metadataFixture) should equal(
        IOSMessagePayload("Manchester United 2-1 Bolton Wanderers\nDavid Beckham 82min (pen)", Map("t" -> "g"))
      )
    }

    "build an appropriate message for a goal event in extra time" in {
      val eventFixture = Goal("David Beckham", manchesterUnited, boltonWanderers, 90, Some("5:23"))

      IOSPayloadBuilder.apply(eventFixture, metadataFixture) should equal(
        IOSMessagePayload("Manchester United 2-1 Bolton Wanderers\nDavid Beckham 90min (+5:23)", Map("t" -> "g"))
      )
    }

    "build an appropriate message for an own goal event in extra time" in {
      val eventFixture = OwnGoal("David Beckham", manchesterUnited, boltonWanderers, 90, Some("1:23"))

      IOSPayloadBuilder.apply(eventFixture, metadataFixture) should equal(
        IOSMessagePayload("Manchester United 2-1 Bolton Wanderers\nDavid Beckham 90min (o.g. +1:23)", Map("t" -> "g"))
      )
    }

    "build an appropriate message for a penalty goal event in extra time" in {
      val eventFixture = PenaltyGoal("David Beckham", manchesterUnited, boltonWanderers, 90, Some("2:00"))

      IOSPayloadBuilder.apply(eventFixture, metadataFixture) should equal(
        IOSMessagePayload("Manchester United 2-1 Bolton Wanderers\nDavid Beckham 90min (pen +2:00)", Map("t" -> "g"))
      )
    }
  }
}
