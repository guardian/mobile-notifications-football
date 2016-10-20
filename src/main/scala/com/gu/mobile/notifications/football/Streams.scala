package com.gu.mobile.notifications.football

import java.time.LocalDate

import com.gu.mobile.notifications.client.models.GoalAlertPayload
import rx.lang.scala.Observable
import com.gu.mobile.notifications.football.models._
import com.gu.mobile.notifications.football.lib._
import org.joda.time.DateTime
import grizzled.slf4j.Logging

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import com.gu.mobile.notifications.football.observables.MatchEventsObservable
import pa.{MatchDay, PaClientErrorsException}
import com.gu.mobile.notifications.football.lib.PaMatchDayClient
import rx.lang.scala.subjects.AsyncSubject

import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait MatchDayStream extends Logging {
  val UpdateInterval: FiniteDuration

  def getMatchDayStream: Observable[MatchDay] = {

    def fullMatchDayStream(tick: Long): Observable[MatchDay] = {
      val subject = AsyncSubject[MatchDay]()

      val todaysMatchesFuture = PaMatchDayClient(PaFootballClient).today

      todaysMatchesFuture onComplete {
        case Success(matchDays) =>
          matchDays.foreach(subject.onNext)
          subject.onCompleted()
        case Failure(pae: PaClientErrorsException) if pae.msg.contains("No data available") =>
          logger.info(s"No match data available for today [${LocalDate.now}]")
        case Failure(e) =>
          logger.error(s"Error getting today's matches from PA [${e.getMessage}]", e)
      }
      subject
    }

    def isLive(matchDay: MatchDay): Boolean = {
      // I voluntarily avoid using the matchDay.liveMatch as we want to start polling slightly before the match starts
      val startPolling = matchDay.date.minusMinutes(15)
      val stopPolling = matchDay.date.plusHours(3)
      startPolling.isAfterNow && stopPolling.isBeforeNow
    }

    Observable.timer(0.seconds, UpdateInterval)
      .flatMap(fullMatchDayStream)
      .filter(isLive)
      .distinct(_.id)
  }
}

trait GoalEventStream extends Logging {
  /** Given a stream of lists of MatchDay results, returns a stream of goal events */
  def getGoalEvents(matchDays: Observable[MatchDay]): Observable[(ScoreEvent, EventFeedMetadata)] = {
    matchDays.flatMap(matchDay => MatchEventsObservable.forMatchId(matchDay.id)) collect {
      case (goal: Goal, metadata) => (goal, metadata)
      case (ownGoal: OwnGoal, metadata) => (ownGoal, metadata)
      case (penalty: PenaltyGoal, metadata) => (penalty, metadata)
    }
  }
}

trait GuardianNotificationStream extends Logging {
  /** Transforms a stream of Goal events into Notifications to be sent via Guardian Mobile Notifications */
  def getGoalEventsAsNotifications(goals: Observable[(ScoreEvent, EventFeedMetadata)]): Observable[GoalAlertPayload] = goals map {
    case (event, metadata) => GoalNotificationBuilder(event, metadata)
  }
}

trait NotificationResponseStream extends Logging {
  this: NotificationsClient =>
  /**
   * retrySendNotifications: If a notification could not be sent, how many times to retry
   */
  val retrySendNotifications: Int

  /** Given a stream of notifications to send, returns a stream of responses from having sent those notifications
    *
    * @param notifications The notifications to send
    * @return The stream
    */
  def getNotificationResponses(notifications: Observable[GoalAlertPayload])(implicit executionContext: ExecutionContext): Observable[NotificationHistoryItem] =
    notifications flatMap { payload =>
      Observable.defer(Observable.from(send(payload))).retry(retrySendNotifications) map {
        case Right(_) => NotificationSent(new DateTime(), payload)
        case Left(e) => NotificationFailed(new DateTime(), payload)
      } onErrorResumeNext {
        Observable.just(NotificationFailed(new DateTime(), payload))
      }
    }
}

trait Streams extends MatchDayStream with GoalEventStream with GuardianNotificationStream with NotificationResponseStream with GuardianNotificationsClient
