package com.gu.mobile.notifications.football.lib

import org.scalatest.{ShouldMatchers, WordSpec}
import Observables._
import rx.lang.scala.Observable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RichObservableSpec extends WordSpec with ShouldMatchers {
  "Observable.pairs" when {
    "applied to an Observable of x | 0 <= x < 10" should {
      "return an Observable of (x, x + 1) | 0 <= x < 9" in {
        Observable.items(0 to 9: _*).pairs.toBlockingObservable.toList shouldEqual (0 to 8).map(i => (i, i + 1)).toList
      }
    }
  }

  "Observable.completeOnError" when {
    "applied to an Observable that emits err" should {
      "complete rather than emitting err" in {
        val err = new Exception("argh!")

        Observable.from(Future.failed(err)).completeOnError.toBlockingObservable.toList shouldEqual Nil
      }
    }
  }

  "Observable.completeOn" should {
    "act like takeWhile on the complement of f, but also emit the first A for which f is true" in {
      val numbers = Observable.items(0 to 20: _*)

      numbers.completeOn(_ > 10).toBlockingObservable.toList shouldEqual (0 to 11).toList
    }
  }

  "Observable.collect" should {
    "act like the standard collect function" in {
      sealed trait A
      case class B(i: Int) extends A
      case class C(s: String) extends A

      Observable.items(B(12), C("hi"), B(4), C("world")).collect({
        case b @ B(_) => b
      }).toBlockingObservable.toList shouldEqual List(B(12), B(4))
    }
  }
}
