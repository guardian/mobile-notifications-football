package com.gu.mobile.notifications.football.lib

import rx.lang.scala.Observable
import grizzled.slf4j.Logging
import Functions.complement

object Observables extends Logging {
  implicit class RichObservable[A](obs: Observable[A]) {
    /** Overlapping pairs */
    def pairs: Observable[(A, A)] = obs.zip(obs.drop(1))

    def completeOnError = obs.onErrorResumeNext(Observable.empty)

    /** Equivalent to takeWhile on the complement of f but also emits the first A for which f is true */
    def completeOn(f: A => Boolean): Observable[A] =
      obs.takeWhile(complement(f)) ++ obs.dropWhile(complement(f)).take(1)

    def collect[B](f: PartialFunction[A, B]): Observable[B] = obs.map(f.lift).filter(_.isDefined).map(_.get)
  }
}
