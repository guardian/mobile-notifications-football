package com.gu.mobile.notifications.football.lib

import org.scalacheck.{Gen, Arbitrary}

/** Generator for arbitrary scorer strings */
object ArbitraryScorerString {
  case class ScorerString(minute: Int, firstName: String, lastName: String) {
    override def toString = s"$firstName $lastName ($minute)"

    def fullName = s"$firstName $lastName"
  }

  implicit val arbitraryScorer = Arbitrary {
    for {
      minute <- Gen.chooseNum(0, 180)
      firstName <- Gen.alphaStr suchThat (_ != "")
      lastName <- Gen.alphaStr suchThat (_ != "")
    } yield ScorerString(minute, firstName, lastName)
  }
}