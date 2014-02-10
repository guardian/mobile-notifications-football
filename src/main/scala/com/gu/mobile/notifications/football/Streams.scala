package com.gu.mobile.notifications.football

import rx.lang.scala.Observable
import com.gu.mobile.notifications.football.models.{NotificationHistoryItem, NotificationFailed, NotificationSent}
import com.gu.mobile.notifications.football.lib._
import org.joda.time.DateTime
import lib.Observables._
import scala.concurrent.ExecutionContext
import pa.MatchDay
import com.gu.mobile.notifications.football.lib.GoalEvent
import com.gu.mobile.notifications.client.models.Notification
import grizzled.slf4j.Logging
import rx.lang.scala.subjects.PublishSubject
import scala.concurrent.duration.FiniteDuration
import ExecutionContext.Implicits.global

trait MatchDayStream extends Logging {
  val UpdateInterval:FiniteDuration

  def getMatchDayStream(): Observable[List[MatchDay]] = {
    // Use the subject below rather than subscribe to the stream directly - otherwise more calls are kicked off to PA than are
    // required
    val stream:Observable[List[MatchDay]] = Observable.interval(UpdateInterval) flatMap { _ =>
      PaMatchDayClient(PaFootballClient).today.asObservable.completeOnError
    }

    val subject = PublishSubject[List[MatchDay]]()
    stream.subscribe(subject)
    subject
  }
}

trait GoalEventStream extends Logging {
  /** Given a stream of lists of MatchDay results, returns a stream of goal events */
  def getGoalEvents(matchDays: Observable[List[MatchDay]]): Observable[GoalEvent] = {
    matchDays.pairs flatMap { case (oldMatchDays, newMatchDays) =>
      Observable((Lists.pairs(oldMatchDays, newMatchDays)(_.id) flatMap { case (oldMatchDay, newMatchDay) =>
        Pa.delta(oldMatchDay, newMatchDay) map { goal => GoalEvent(goal, newMatchDay) }
      }).toList: _*)
    }
  }
}

trait GuardianNotificationStream extends Logging {
  /** Transforms a stream of Goal events into Notifications to be sent via Guardian Mobile Notifications */
  def getGoalEventsAsNotifications(goals: Observable[GoalEvent]): Observable[Notification] = goals map {
    case GoalEvent(goal, matchDay) => GoalNotificationBuilder(goal, matchDay)
  }
}

trait NotificationResponseStream extends NotificationsClient with Logging {
  /**
   * retrySendNotifications: If a notification could not be sent, how many times to retry
   */
  val retrySendNotifications: Int

  /** Given a stream of notifications to send, returns a stream of responses from having sent those notifications
    *
    * @param notifications The notifications to send
    * @return The stream
    */
  def getNotificationResponses(notifications: Observable[Notification])(implicit executionContext: ExecutionContext): Observable[NotificationHistoryItem] =
    notifications flatMap { notification =>
      Observable.defer(send(notification).asObservable).retry(retrySendNotifications) map { reply =>
        NotificationSent(
          new DateTime(),
          notification,
          reply
        )
      } onErrorResumeNext {
        Observable(NotificationFailed(
          new DateTime(),
          notification
        ))
      }
    }
}

trait Streams extends MatchDayStream with GoalEventStream with GuardianNotificationStream with NotificationResponseStream
