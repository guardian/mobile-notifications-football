package com.gu.mobile.notifications.football.lib

import org.scalatest.{ShouldMatchers, WordSpec}
import Observables._
import rx.lang.scala.Observable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RichObservableSpec extends WordSpec with ShouldMatchers {
  "RichObservable.repeatedly" when {
    "applied to a Future(1)" should {
      "return an infinite Observable of 1" in {
        Observable.repeatedly(Future.successful(1)).take(10).toBlockingObservable.toList shouldEqual List.fill(10)(1)
      }
    }
    "applied to a Future(n), where n is initially 0 and increments after each subsequent application" should {
      "return an infinite Observable of incrementing integers" in {
        var n = 0

        Observable.repeatedly({
          val ftr = Future.successful(n)
          n += 1
          ftr
        }).take(10).toBlockingObservable.toList shouldEqual (0 to 9).toList
      }
    }
  }
}
