package com.gu.mobile.notifications.football.lib

import rx.lang.scala.{Subscription, Observable}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import grizzled.slf4j.Logging

object Observables extends Logging {
  implicit class RichObservableCompanion(obs: Observable.type) {
    val empty = Observable(List(): _*)
  }

  implicit class RichObservable[A](obs: Observable[A]) {
    /** Overlapping pairs */
    def pairs: Observable[(A, A)] = obs.zip(obs.drop(1))

    def completeOnError = obs.onErrorResumeNext(Observable.empty)
  }

  implicit class RichFuture[A](f: Future[A]) {
    def asObservable(implicit executionContext: ExecutionContext): Observable[A] = {
      Observable { o =>
        val subs = Subscription()

        f onComplete {
          case Success(s) =>
            o.onNext(s)
            o.onCompleted()
          case Failure(s) =>
            logger.info("Error completing future -  " + s)
            o.onError(s)
        }

        subs
      }
    }
  }
}
