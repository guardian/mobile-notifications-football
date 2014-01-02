package com.gu.mobile.notifications.football.lib

import org.scalatest.WordSpec
import org.scalacheck.Prop.forAll

class ListsSpec extends WordSpec {
  "Lists.pairs" when {
    "applied to A, B, and id" should {
      "equal { (x, x) | x ∊ A, x ∊ B }" in {
        forAll { (xs: List[Int], ys: List[Int]) =>
          Lists.pairs(xs, ys)(identity) == (xs.toSet intersect ys.toSet).map(x => (x, x))
        }
      }
    }
  }
}
