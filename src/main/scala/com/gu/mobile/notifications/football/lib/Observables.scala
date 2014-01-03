package com.gu.mobile.notifications.football.lib

import rx.lang.scala.{Subscription, Observer, Observable}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Observables {
  implicit class RichObservableCompanion(obs: Observable.type) {
    /**
     * Given a block that evaluates to a future, returns the observable of repeatedly evaluating that future
     * (waiting for the previous future to complete before continuing).
     *
     * @param f The block
     * @tparam A The type of the observable
     * @return The observable
     */
    def repeatedly[A](f: => Future[A])(implicit executionContext: ExecutionContext): Observable[A] = {
      Observable { observer: Observer[A] =>
        val subscription = Subscription()

        def repeatOver(future: Future[A]) {
          def triggerNext() = if (!subscription.isUnsubscribed) repeatOver(f)

          future onComplete {
            case Success(a) => {
              observer.onNext(a)
              triggerNext()
            }
            case _ => triggerNext()
          }
        }

        repeatOver(f)

        subscription
      }
    }

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
          case Success(s) => {
            o.onNext(s)
            o.onCompleted()
          }
          case Failure(s) => o.onError(s)
        }

        subs
      }
    }
  }
}
