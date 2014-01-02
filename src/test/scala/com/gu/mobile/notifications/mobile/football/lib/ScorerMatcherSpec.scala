package com.gu.mobile.notifications.football.lib

import org.specs2._
import ArbitraryScorerString._

class ScorerMatcherSpec extends Specification with ScalaCheck {
  def is =
    "match string" ! prop { (scorer: ScorerString) => Pa.ScorerMatcher.findFirstMatchIn(scorer.toString).isDefined } ^
    "extract name" ! prop { (scorer: ScorerString) =>
      Pa.ScorerMatcher.findFirstMatchIn(scorer.toString).get.group(1) mustEqual scorer.fullName
    } ^
    "extract minute" ! prop { (scorer: ScorerString) =>
      Pa.ScorerMatcher.findFirstMatchIn(scorer.toString).get.group(2).toInt mustEqual scorer.minute
    }
}
