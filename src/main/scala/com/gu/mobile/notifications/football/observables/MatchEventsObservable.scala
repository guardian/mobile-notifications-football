package com.gu.mobile.notifications.football.observables

import com.gu.mobile.notifications.football.lib.{MatchDayClient, PaFootballClient, PaMatchDayClient}
import com.gu.mobile.notifications.football.models.{EventFeedMetadata, MatchEvent, MatchEventTeam, Result}
import EventFeedMetadata._
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Random, Success}
import grizzled.slf4j.Logging
import pa.MatchEvents
import rx.lang.scala.subjects.PublishSubject

object MatchEventsObservable extends MatchEventsObservableLogic with Logging {
  val apiClient = PaMatchDayClient(PaFootballClient)

  val pollingIntervalTolerance = 5.seconds

  val pollingInterval = 30.seconds
}

trait MatchEventsObservableLogic { self: Logging =>

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

  type MatchEventMetadata = (MatchEvent, EventFeedMetadata)

  /** Polls the PA event feed every polling iteration (+ some random tolerance, to prevent our sending blocks of calls
    * to PA all at once) until the match has finished
    *
    * @param id The ID of the match
    * @return The observable
    */
  def eventFeeds(id: String): Observable[MatchEventMetadata] = {

    def withMetadata(events: MatchEvents): Seq[MatchEventMetadata] = {
      MatchEvent.fromMatchEvents(events)
        .zipWithMetadata(
          matchID = id,
          homeTeam = MatchEventTeam.fromTeam(events.homeTeam),
          awayTeam = MatchEventTeam.fromTeam(events.awayTeam)
        )
    }

    def pushEvents(subject: Subject[MatchEventMetadata]): PartialFunction[MatchEventMetadata, Unit] = {
      case (Result, _) =>
        logger.info(s"$id >>> match finished!")
        subject.onCompleted()
      case (event, metadata) => subject.onNext(event -> metadata)
    }

    def fullEventStream(tick: Long): Observable[MatchEventMetadata] = {
      val subject = PublishSubject[MatchEventMetadata]

      apiClient.matchEvents(id) onComplete {
        case Success(events) =>
          logger.debug(s"$id >>> got ${events.events.length} events from feed")
          withMetadata(events).foreach(pushEvents(subject))
        case Failure(error) =>
          logger.warn(s"$id >>> Error when polling: ", error)
      }

      subject
    }

    val pollingInterval = randomPollingInterval
    logger.info(s"$id >>> polling every $pollingInterval")

    val randomStart = Random.nextInt(pollingIntervalTolerance.toSeconds.toInt).seconds
    Observable.timer(randomStart, pollingInterval)
      .flatMap(fullEventStream)
      .distinct { case (event, _) => event.id }
  }

  /** An observable for all events for the match with the given ID.
    *
    * @param id The match ID
    * @return The observable
    */
  def forMatchId(id: String): Observable[(MatchEvent, EventFeedMetadata)] = eventFeeds(id)
}
