package com.gu.mobile.notifications.football.lib

import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class CachedValue[T](timeout: FiniteDuration) {
  var value: Option[T] = None
  var expiry: Option[DateTime] = None

  def apply()(fn: => Future[T])(implicit ec: ExecutionContext): Future[T] = (for {
    v <- value
    exp <- expiry if exp isAfter DateTime.now
  } yield {
    Future.successful(v)
  }) getOrElse {
    fn.andThen {
      case Success(m) =>
        value = Some(m)
        expiry = Some(DateTime.now.plusSeconds(timeout.toSeconds.toInt))
      case Failure(e) =>
    }
  }
}