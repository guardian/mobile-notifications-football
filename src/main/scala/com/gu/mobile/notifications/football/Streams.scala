package com.gu.mobile.notifications.football

import rx.lang.scala.{Subject, Observable}
import com.gu.mobile.notifications.football.models._
import com.gu.mobile.notifications.football.lib._
import org.joda.time.DateTime
import lib.Observables._
import scala.concurrent.ExecutionContext
import grizzled.slf4j.Logging
import scala.concurrent.duration.FiniteDuration
import ExecutionContext.Implicits.global
import com.gu.mobile.notifications.football.observables.MatchEventsObservable
import pa.MatchDay
import com.gu.mobile.notifications.football.lib.PaMatchDayClient
import com.gu.mobile.notifications.client.models.Notification

trait MatchDayStream extends Logging {
  val UpdateInterval: FiniteDuration

  def getMatchDayStream: Observable[List[MatchDay]] = {
    // Use the subject below rather than subscribe to the stream directly - otherwise more calls are kicked off to PA
    // than are required
    val stream: Observable[List[MatchDay]] = Observable.interval(UpdateInterval) flatMap { _ =>
      Observable.from(PaMatchDayClient(PaFootballClient).today).completeOnError
    }

    val subject = Subject[List[MatchDay]]()
    stream.subscribe(subject)
    subject
  }

  def matchIdSets(matchDays: Observable[List[MatchDay]]): Observable[Set[String]] = {
    /** The set of match IDs will change exactly once per day. No two sets will contain the same match IDs.
      * This ensures that the observable does not emit the same IDs repeatedly (so when we flatMap it later into match
      * event Observables we don't end up creating duplicates) but also ensures that the Observable always occupies
      * constant space (as it only remembers the previously seen set of IDs, rather than every ID it's ever seen).
      */
    matchDays.map(_.map(_.id).toSet).distinctUntilChanged
  }
}

trait GoalEventStream extends Logging {
  /** Given a stream of lists of MatchDay results, returns a stream of goal events */
  def getGoalEvents(matchIdSets: Observable[Set[String]]): Observable[(ScoreEvent, EventFeedMetadata)] = {
    matchIdSets flatMap { ids =>
      ids.map(MatchEventsObservable.forMatchId).fold(Observable.empty)(_.merge(_)) collect {
        case (goal @ Goal(_, _, _, _, _), metadata) => (goal, metadata)
        case (ownGoal @ OwnGoal(_, _, _, _, _), metadata) => (ownGoal, metadata)
        case (penalty @ PenaltyGoal(_, _, _, _, _), metadata) => (penalty, metadata)
      }
    }
  }
}

trait GuardianNotificationStream extends Logging {
  /** Transforms a stream of Goal events into Notifications to be sent via Guardian Mobile Notifications */
  def getGoalEventsAsNotifications(goals: Observable[(ScoreEvent, EventFeedMetadata)]): Observable[Notification] = goals map {
    case (event, metadata) => GoalNotificationBuilder(event, metadata)
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
      Observable.defer(Observable.from(send(notification))).retry(retrySendNotifications) map { reply =>
        NotificationSent(
          new DateTime(),
          notification,
          reply
        )
      } onErrorResumeNext {
        Observable.items(NotificationFailed(
          new DateTime(),
          notification
        ))
      }
    }
}

trait Streams extends MatchDayStream with GoalEventStream with GuardianNotificationStream with NotificationResponseStream
