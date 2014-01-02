package com.gu.mobile.notifications.football.lib

import pa.MatchDayTeam
import com.gu.mobile.notifications.football.lib.Pa._
import ArbitraryMatchDayTeam._
import ArbitraryScorerString._
import com.gu.mobile.notifications.football.helpers
import org.scalatest.WordSpec
import org.scalacheck.Prop.forAll

class RichMatchDayTeamSpec extends WordSpec {
  implicit val arbitraryGoalSet = helpers.ScalaCheck.arbContainerOfMaxSize[Set, ScorerString](20)

  "RichMatchDayTeam.goals" when {
    "applied to a match day team" should {
      "return a list of goals scored by that team" in {
        forAll { (initialFixture: MatchDayTeam, scorerStrings: Set[ScorerString]) =>
          val matchDayTeam = initialFixture.copy(scorers=Some(scorerStrings.map(_.toString).mkString(", ")))

          matchDayTeam.goals.toSet == scorerStrings.map(scorer => Goal(scorer.minute, scorer.fullName, matchDayTeam))
        }
      }
    }
  }
}
