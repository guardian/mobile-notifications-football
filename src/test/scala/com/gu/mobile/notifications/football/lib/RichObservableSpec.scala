package com.gu.mobile.notifications.football.lib

import org.scalatest.{ShouldMatchers, WordSpec}
import Observables._
import rx.lang.scala.Observable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RichObservableSpec extends WordSpec with ShouldMatchers {
  "Observable.repeatedly" when {
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

  "Observable.empty" should {
    "immediately return" in {
      Observable.empty.toBlockingObservable.toList shouldEqual Nil
    }
  }

  "Observable.pairs" when {
    "applied to an Observable of x | 0 <= x < 10" should {
      "return an Observable of (x, x + 1) | 0 <= x < 9" in {
        Observable(0 to 9).pairs.toBlockingObservable.toList shouldEqual (0 to 8).map(i => (i, i + 1)).toList
      }
    }
  }

  "Future.asObservable" when {
    "applied to Future.successful(1)" should {
      "return an Observable containing a single '1'" in {
        Future.successful(1).asObservable.toBlockingObservable.toList shouldEqual List(1)
      }
    }

    "applied to Future.failed(err)" should {
      "return an Observable that fails on err" in {
        val err = new Exception("hi")

        intercept[Exception] {
          Future.failed(err).asObservable.toBlockingObservable.toList
        }
      }
    }
  }

  "Observable.completeOnError" when {
    "applied to an Observable that emits err" should {
      "complete rather than emitting err" in {
        val err = new Exception("argh!")

        Future.failed(err).asObservable.completeOnError.toBlockingObservable.toList shouldEqual Nil
      }
    }
  }
}
