package com.gu.mobile.notifications.football.lib

import org.specs2.{ScalaCheck, Specification}
import org.scalacheck.Gen

class IntegerStringSpec extends Specification with ScalaCheck {
  implicit val arbitraryNonDigitString = Gen.alphaStr

  def is = "convert integer strings into integers" ! prop { n: Int =>
    IntegerString.unapply(n.toString) mustEqual Some(n)
  } ^ "return None for non-integer strings" ! prop { s: String =>
    IntegerString.unapply(s) mustEqual None
  }
}
