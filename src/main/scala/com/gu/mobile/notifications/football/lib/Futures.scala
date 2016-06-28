package com.gu.mobile.notifications.football.lib

import scala.concurrent.{ExecutionContext, Future}

object Futures {
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
