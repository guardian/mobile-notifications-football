package com.gu.mobile.notifications.football

import com.gu.mobile.notifications.client.models.Notification
import rx.lang.scala.Observable
import com.gu.mobile.notifications.football.models.{NotificationHistoryItem, NotificationFailed, NotificationSent}
import com.gu.mobile.notifications.football.lib.{SendsNotifications, GoalNotificationBuilder, GoalEvent}
import org.joda.time.DateTime
import lib.Observables._
import scala.concurrent.ExecutionContext

object Streams {
  def notifications(goals: Observable[GoalEvent]): Observable[Notification] = goals map {
    case GoalEvent(goal, matchDay) => GoalNotificationBuilder(goal, matchDay)
  }

  /** Given a stream of notifications to send, returns a stream of responses from having sent those notifications
    *
    * @param notifications The notifications to send
    * @param retries If a notification could not be sent, how many times to retry
    * @return The stream
    */
  def notificationResponses(
      notifications: Observable[Notification],
      client: SendsNotifications,
      retries: Int
  )(implicit executionContext: ExecutionContext): Observable[NotificationHistoryItem] =
    notifications flatMap { notification =>
      Observable.defer(client.send(notification).asObservable).retry(retries) map { reply =>
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
