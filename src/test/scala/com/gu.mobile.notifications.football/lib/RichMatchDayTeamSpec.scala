package com.gu.mobile.notifications.football.lib

import org.specs2.{Specification, ScalaCheck}
import pa.MatchDayTeam
import com.gu.mobile.notifications.football.lib.Pa._
import ArbitraryMatchDayTeam._
import ArbitraryScorerString._
import com.gu.mobile.notifications.football.helpers

class RichMatchDayTeamSpec extends Specification with ScalaCheck {
  implicit val arbitraryGoalSet = helpers.ScalaCheck.arbContainerOfMaxSize[Set, ScorerString](20)

  def is = "goals" ! prop { (initialFixture: MatchDayTeam, scorerStrings: Set[ScorerString]) =>
    val matchDayTeam = initialFixture.copy(scorers=Some(scorerStrings.map(_.toString).mkString(", ")))

    matchDayTeam.goals.toSet mustEqual scorerStrings.map(scorer => Goal(scorer.minute, scorer.fullName, matchDayTeam))
  }
}
