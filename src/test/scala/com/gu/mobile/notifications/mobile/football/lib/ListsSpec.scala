package com.gu.mobile.notifications.football.lib

import org.specs2.{ScalaCheck, Specification}

class ListsSpec extends Specification with ScalaCheck {
  def is = "pairs" ! check { (xs: List[Int], ys: List[Int]) =>
    Lists.pairs(xs, ys)(identity) == (xs.toSet intersect ys.toSet).map(x => (x, x))
  }
}
