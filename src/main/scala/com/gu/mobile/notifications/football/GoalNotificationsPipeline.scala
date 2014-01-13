package com.gu.mobile.notifications.football

import com.gu.mobile.notifications.football.lib._
import scala.concurrent.ExecutionContext.Implicits.global
import rx.lang.scala.Observable
import lib.Observables._
import rx.lang.scala.subjects.PublishSubject
import pa.MatchDay
import scala.concurrent.duration._
import com.gu.mobile.notifications.football.models.{NotificationFailed, NotificationSent}
import grizzled.slf4j.Logging
import lib.Pa._
import com.gu.mobile.notifications.client.models.Notification
import scala.util.{Failure, Success}

object GoalNotificationsPipeline extends Logging {
  val UpdateInterval = 3.seconds
  val MaxHistoryLength = 100
  val RetrySendNotifications = 5
  
  // Use the subject below rather than subscribe to this directly - otherwise more calls are kicked off to PA than are
  // required
  val matchDayStream = Observable.interval(UpdateInterval).flatMap(
    const(PaMatchDayClient(PaFootballClient).today.asObservable.completeOnError)
  )

  val matchDayPublishSubject = PublishSubject[List[MatchDay]]()
  matchDayStream.subscribe(matchDayPublishSubject)

  val goalEventStream = Streams.goalEvents(matchDayPublishSubject)
  val goalEventSubject = PublishSubject[GoalEvent]()
  goalEventStream.subscribe(goalEventSubject)

  val notificationStream = Streams.guardianNotifications(goalEventSubject)

  val notificationPublishSubject = PublishSubject[Notification]()
  notificationStream.subscribe(notificationPublishSubject)

  val notificationSendHistory = Streams.notificationResponses(
    notificationPublishSubject, NotificationsClient, RetrySendNotifications)

  def start() {
    notificationSendHistory subscribe { notificationResponse =>
      Agents.notificationsHistory.sendOff(history => (notificationResponse :: history) take MaxHistoryLength)

      // TODO better logging ... really
      notificationResponse match {
        case NotificationSent(when, notification, _) => info(s"Sent notification at $when")
        case NotificationFailed(when, notification) => info(s"Failed to send notification at $when")
      }
    }

    goalEventSubject subscribe { goalEvent =>
      logger.info(s"Goal event: ${goalEvent.goal.scorerName} scored for ${goalEvent.goal.scoringTeam.name} at " +
        s"minute ${goalEvent.goal.minute}")
    }

    matchDayPublishSubject subscribe { matchDays =>
      info(s"Got new set of match days: ${matchDays.map(_.summaryString).mkString(", ")}")
      Agents.lastMatchDaysSeen send const(Some(matchDays))
    }

    notificationPublishSubject subscribe { notification =>
      SNSQueue.publish("Goal notification created", notification.toString) match {
        case Success(publishResult) =>
          logger.info(s"Successfully sent notification to SNS queue: ${publishResult.getMessageId}")
        case Failure(error) =>
          logger.error(s"Encountered error when trying to add notification to SNS queue", error)
      }
    }
  }
}
