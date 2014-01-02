package com.gu.mobile.notifications.football.lib

import ArbitraryScorerString._
import org.scalatest.WordSpec
import org.scalacheck.Prop.forAll

class ScorerMatcherSpec extends WordSpec {
  "ScorerMatcher" when {
    "applied to a scorer string" should {
      "match it" in {
        forAll { (scorer: ScorerString) => Pa.ScorerMatcher.findFirstMatchIn(scorer.toString).isDefined }
      }
      "extract the Scorer's name" in {
        forAll { (scorer: ScorerString) =>
          Pa.ScorerMatcher.findFirstMatchIn(scorer.toString).get.group(1) == scorer.fullName
        }
      }
      "extract the minute of the goal" in {
        forAll { (scorer: ScorerString) =>
          Pa.ScorerMatcher.findFirstMatchIn(scorer.toString).get.group(2).toInt == scorer.minute
        }
      }
    }
  }
}
