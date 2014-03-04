package com.gu.mobile.notifications.football.observables

import com.gu.mobile.notifications.football.lib.{MatchDayClient, PaMatchDayClient, PaFootballClient}
import com.gu.mobile.notifications.football.lib.Observables._
import com.gu.mobile.notifications.football.models.{MatchEventTeam, EventFeedMetadata, MatchEvent}
import EventFeedMetadata._
import rx.lang.scala.Observable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import pa.MatchEvents
import scala.util.{Failure, Success, Random}
import grizzled.slf4j.Logging

object MatchEventsObservable extends MatchEventsObservableLogic with Logging {
  val apiClient = PaMatchDayClient(PaFootballClient)

  val pollingIntervalTolerance = 5.seconds

  val pollingInterval = 15.seconds
}

object MatchEventsObservableLogic {
  def eventsFromFeeds(id: String, observable: Observable[MatchEvents]): Observable[(MatchEvent, EventFeedMetadata)] = {
    val eventFeeds = observable map { matchEvents =>
      MatchEvent.fromMatchEvents(matchEvents).zipWithMetadata(
        id,
        MatchEventTeam.fromTeam(matchEvents.homeTeam),
        MatchEventTeam.fromTeam(matchEvents.awayTeam)
      )
    }

    (Observable.items(Seq.empty[(MatchEvent, EventFeedMetadata)]) ++ eventFeeds).pairs flatMap { case (events1, events2) =>
      /** This should *theoretically* be good enough as the events should always occur in the same order
        *
        * I do, however, imagine I'll be revisiting this code at some point ...
        */
      Observable.items(events2.drop(events1.length): _*)
    }
  }
}

trait MatchEventsObservableLogic { self: Logging =>
  import MatchEventsObservableLogic._

  protected val pollingInterval: FiniteDuration

  protected val pollingIntervalTolerance: FiniteDuration

  protected val apiClient: MatchDayClient

  /** We use a random polling interval so that it's unlikely all the calls for match events updates fall at exactly
    * the same time
    *
    * @return The interval
    */
  private def randomPollingInterval =
    pollingInterval + Random.nextInt(pollingIntervalTolerance.toMillis.toInt).millis

  /** Polls the PA event feed every polling iteration (+ some random tolerance, to prevent our sending blocks of calls
    * to PA all at once) until the match has finished
    *
    * @param id The ID of the match
    * @return The observable
    */
  def eventFeeds(id: String): Observable[MatchEvents] = {
    val pollingInterval = randomPollingInterval

    logger.info(s"$id >>> polling every $pollingInterval")

    (Observable.interval(pollingInterval) flatMap { _ =>
      val eventsFuture = apiClient.matchEvents(id)

      eventsFuture onComplete {
        case Success(events) =>
          logger.debug(s"$id >>> got ${events.events.length} events from feed")
          if (events.isResult) {
            logger.info(s"$id >>> match finished!")
          }
        case Failure(error) =>
          logger.warn(s"$id >>> Error when polling: ", error)
      }

      Observable.from(eventsFuture).completeOnError
    }).completeOn(_.isResult)
  }

  /** An observable for all events for the match with the given ID.
    *
    * @param id The match ID
    * @return The observable
    */
  def forMatchId(id: String): Observable[(MatchEvent, EventFeedMetadata)] = eventsFromFeeds(id, eventFeeds(id))
}
