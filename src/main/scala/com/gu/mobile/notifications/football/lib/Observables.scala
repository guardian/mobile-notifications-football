package com.gu.mobile.notifications.football.lib

import rx.lang.scala.{Subscription, Observer, Observable}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object Observables {
  implicit class RichObservable(obs: Observable.type) {
    /**
     * Given a block that evaluates to a future, returns the observable of repeatedly evaluating that future
     * (waiting for the previous future to complete before continuing).
     *
     * @param f The block
     * @tparam A The type of the observable
     * @return The observable
     */
    def repeatedly[A](f: => Future[A])(implicit executionContext: ExecutionContext): Observable[A] = {
      Observable({ observer: Observer[A] =>
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
      })
    }
  }
}
