package com.gu.mobile.notifications.football.lib

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalatest.WordSpec

class IntegerStringSpec extends WordSpec {
  implicit val arbitraryNonDigitString = Gen.alphaStr

  "IntegerString" when {
    "given an integer as a string" should {
      "return it as an integer" in {
        forAll { n: Int => IntegerString.unapply(n.toString) == Some(n) }
      }
    }
    "given some other string" should {
      "return None" in {
        forAll { s: String => IntegerString.unapply(s) == None  }
      }
    }
  }
}
