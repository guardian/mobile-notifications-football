package com.gu.mobile.notifications.football.models

import org.scalatest.{Matchers, WordSpec}
import EventFeedMetadata._

class EventFeedMetaDataSpec extends WordSpec with Matchers {
  val homeTeam = MatchEventTeam(14, "Norwich")
  val awayTeam = MatchEventTeam(2, "Aston Villa")
  val matchID = "3704151"

  val eventsList = List(
    KickOff,
    Goal("Wes Hoolahan", homeTeam, awayTeam, 3),
    Goal("Christian Benteke", awayTeam, homeTeam, 25),
    Goal("Christian Benteke", awayTeam, homeTeam, 27),
    Goal("Leandro Bacuna", awayTeam, homeTeam, 37),
    OwnGoal("Sebastien Bassong", awayTeam, homeTeam, 41),
    Result
  )

  val withMetaData = eventsList.zipWithMetadata(matchID, homeTeam, awayTeam)

  "zipWithMetadata" should {
    "correctly report scores throughout the match" in {
      withMetaData map {
        case (_, metaData) => (metaData.homeTeamScore, metaData.awayTeamScore)
      } should equal(List(
        (0, 0),
        (1, 0),
        (1, 1),
        (1, 2),
        (1, 3),
        (1, 4),
        (1, 4)
      ))
    }

    "always report the correct home team" in {
      withMetaData.map(_._2.homeTeam).distinct should be(List(homeTeam))
    }

    "always report the correct away team" in {
      withMetaData.map(_._2.awayTeam).distinct should be(List(awayTeam))
    }

    "always report the correct match ID" in {
      withMetaData.map(_._2.matchID).distinct should be(List(matchID))
    }
  }
}
