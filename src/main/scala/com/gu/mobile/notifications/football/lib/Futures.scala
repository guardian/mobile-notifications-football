package com.gu.mobile.notifications.football.lib

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import com.gu.management.TimingMetric

object Futures {
  implicit class RichFuture[A](val future: Future[A]) {
    def measure(f: (Try[A], Long) => Unit)(implicit context: ExecutionContext) {
      val now = System.currentTimeMillis
      def elapsed = System.currentTimeMillis - now

      future onComplete { case a => f(a, elapsed) }
    }
    
    def recordTimeSpent(success: TimingMetric, failure: TimingMetric)(implicit context: ExecutionContext) {
      measure {
        case (Success(_), elapsed) => success.recordTimeSpent(elapsed)
        case (Failure(_), elapsed) => failure.recordTimeSpent(elapsed)
      }
    }
  }

  implicit class RichFutureCompanion(companion: Future.type) {
    /** Like Future.sequence, but silently ignores failures, collecting all successful results */
    def sequenceSuccessful[A](futures: Seq[Future[A]])(implicit executionContext: ExecutionContext): Future[List[A]] = {
      def iter(futures: Seq[Future[A]], acc: List[A]): Future[List[A]] = {
        futures match {
          case fa :: fas => fa flatMap { a: A =>
            iter(fas, a :: acc)
          } recoverWith {
            case _ => iter(fas, acc)
          }
          case Nil => Future.successful(acc.reverse)
        }
      }

      iter(futures, Nil)
    }
  }
}
