package com.gu.mobile.notifications.football.helpers

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary._
import scala.Some

object ScalaCheck {
  implicit class RichGen(gen: Gen.type) {
    /** Newer versions of ScalaCheck come with this method. We can't upgrade yet due to problems with dependencies
      * clashing between the newer versions of Specs2 & Spray
      */
    def option[T](gen: Gen[T]): Gen[Option[T]] = Gen.oneOf(gen.map(Some.apply), None)
  }

  def arbContainerOfMaxSize[C[_], T: Arbitrary](maxSize: Int) = Arbitrary {
    for {
      n <- Gen.chooseNum(0, 20)
      set <- Gen.containerOfN[Set, T](n, arbitrary[T])
    } yield set
  }
}
