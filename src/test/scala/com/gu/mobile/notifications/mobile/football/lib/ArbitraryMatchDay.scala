package com.gu.mobile.notifications.football.lib

import org.scalacheck.{Gen, Arbitrary}
import com.gu.mobile.notifications.football.helpers.ScalaCheck._
import Arbitrary._
import pa._
import org.joda.time.DateTime

object ArbitraryMatchDayTeam {
  implicit val arbitraryMatchDayTeam = Arbitrary {
    val optScore = Gen.option(Gen.chooseNum(0, 10))

    for {
      id <- Gen.alphaStr
      name <- Gen.alphaStr
      score <- optScore
      htScore <- optScore
      aggregateScore <- optScore
      scorerStr <- Gen.option(Gen.alphaStr)
    } yield MatchDayTeam(id, name, score, htScore, aggregateScore, scorerStr)
  }
}

object ArbitraryMatchDay {
  import ArbitraryMatchDayTeam._

  implicit val arbitraryCompetition = Arbitrary {
    for {
      id <- Gen.alphaStr
      name <- Gen.alphaStr
    } yield Competition(id, name)
  }

  implicit val arbitraryOfficial = Arbitrary {
    for {
      id <- Gen.alphaStr
      name <- Gen.alphaStr
    } yield Official(id, name)
  }

  implicit val arbitraryRound = Arbitrary {
    for {
      roundNumber <- Gen.numStr
      name <- Gen.option(Gen.alphaStr)
    } yield Round(roundNumber, name)
  }

  implicit val arbitraryVenue = Arbitrary {
    for {
      id <- Gen.alphaStr
      name <- Gen.alphaStr
    } yield Venue(id, name)
  }

  implicit val arbitraryMatchDay = Arbitrary {
    for {
      id <- Gen.alphaStr
      competition <- Gen.option(arbitrary[Competition])
      round <- Gen.option(arbitrary[Round])
      leg <- Gen.alphaStr
      liveMatch <- arbitrary[Boolean]
      result <- arbitrary[Boolean]
      previewAvailable <- arbitrary[Boolean]
      reportAvailable <- arbitrary[Boolean]
      lineUpAvailable <- arbitrary[Boolean]
      matchStatus <- arbitrary[String]
      attendance <- Gen.option(arbitrary[String])
      homeTeam <- arbitrary[MatchDayTeam]
      awayTeam <- arbitrary[MatchDayTeam]
      referee <- Gen.option(arbitrary[Official])
      venue <- Gen.option(arbitrary[Venue])
      comments <- Gen.option(Gen.alphaStr)
    } yield MatchDay(
      id,
      new DateTime,
      competition,
      round,
      leg,
      liveMatch,
      result,
      previewAvailable,
      reportAvailable,
      lineUpAvailable,
      matchStatus,
      attendance,
      homeTeam,
      awayTeam,
      referee,
      venue,
      comments
    )
  }
}