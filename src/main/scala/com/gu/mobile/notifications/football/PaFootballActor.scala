package com.gu.mobile.notifications.football

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import akka.actor.{Actor, ActorRef}
import org.joda.time.DateTime
import com.gu.mobile.notifications.football.lib.{CachedValue, DynamoDistinctCheck, PaFootballClient, SyntheticMatchEventGenerator}
import DynamoDistinctCheck.{Distinct, Duplicate, Unknown}
import com.gu.Logging
import com.gu.mobile.notifications.football.models.MatchId
import pa.{MatchDay, MatchEvent}

object PaFootballActor {
  case class EndedMatch(matchId: String, startTime: DateTime)
  case object TriggerPoll
  case class PollFinished(senderRef: ActorRef)
  case class ProcessedEvents(events: Set[String], senderRef: ActorRef)
}

class PaFootballActor(
  paClient: PaFootballClient,
  distinctCheck: DynamoDistinctCheck,
  syntheticEvents: SyntheticMatchEventGenerator,
  eventConsumer: EventConsumer
) extends Actor with Logging {

  implicit val ec = context.dispatcher

  import PaFootballActor._

  private var ready: Boolean = true
  private var processedEvents = Set.empty[String]
  private var endedMatches: Set[EndedMatch] = Set.empty
  private val cachedMatches = new CachedValue[List[MatchDay]](30.minutes)

  def receive = {
    case TriggerPoll if !ready =>
      logger.warn("Poll triggered while existing poll still in progress, ignoring")

    case TriggerPoll if ready =>
      val senderRef = sender()
      ready = false
      logger.info("Starting poll for new match events")

      val matchEventIds = for {
        liveMatches <- matchIdsInProgress
        ids <- Future.traverse(liveMatches)(processMatch)
      } yield ids.flatten.toSet

      matchEventIds andThen {
        case Success(ids) =>
          logger.info("Finished polling with success")
          self ! ProcessedEvents(ids, senderRef)
        case Failure(e) =>
          logger.error(s"Finished polling with error ${e.getMessage}")
          self ! PollFinished(senderRef)
      }

    case ProcessedEvents(events, senderRef) =>
      processedEvents = events
      self ! PollFinished(senderRef)

    case PollFinished(senderRef) =>
      ready = true
      logger.info("Stopping lambda")
      senderRef ! "done"

    case endedMatch: EndedMatch =>
      endedMatches = endedMatches.filter(_.startTime.isAfter(DateTime.now.minusDays(2))) + endedMatch
  }

  private def matchIdsInProgress: Future[List[MatchId]] = {
    def inProgress(m: MatchDay): Boolean =
      m.date.minusMinutes(5).isBeforeNow &&
        (!endedMatches.exists(_.matchId == m.id) || m.date.plusHours(4).isAfterNow)
    val matches = cachedMatches() {
      logger.info("Retrieving today's matches from PA")
      paClient.aroundToday recover {
        case e =>
          logger.error("Error retrieving today's matches from PA", e)
          cachedMatches.value.getOrElse(List.empty)
      }
    }
    matches.map(_.filter(inProgress).map(_.id).map(MatchId))
  }

  private def processMatch(matchId: MatchId): Future[List[String]] = {
    val eventIds = for {
      (matchDay, events) <- paClient.eventsForMatch(matchId, syntheticEvents)
      processed <- Future.traverse(events)(processMatchEvent(matchDay, events))
    } yield processed.flatten
    eventIds.recover { case e =>
        logger.error(s"Failed to process match ${matchId.id}: ${e.getMessage}", e)
        List.empty[String]
    }
  }

  private def processMatchEvent(matchDay: MatchDay, events: List[MatchEvent])(event: MatchEvent): Future[Option[String]] = {
    handleMatchEnd(matchDay, event)

    event.id.filterNot(processedEvents.contains).map { id =>
      distinctCheck.insertEvent(id, event).flatMap {
        case Distinct => uniqueEvent(matchDay, events)(event)
        case Duplicate => Future.successful(event.id)
        case Unknown => Future.successful(None)
      }
    } getOrElse {
      logger.debug(s"Event ${event.id} already exists in local cache or does not have an id - discarding")
      Future.successful(event.id)
    }
  }

  private def uniqueEvent(matchDay: MatchDay, events: List[MatchEvent])(event: MatchEvent): Future[Option[String]] = {
    logger.info(s"Found unique event $event")
    val previousEvents = events.takeWhile(_ != event)
    eventConsumer.receiveEvent(matchDay, previousEvents, event).recover({case _ => ()}).map(_ => event.id)
  }

  private def handleMatchEnd(matchDay: MatchDay, event: MatchEvent) = {
    if (event.eventType == "full-time") {
      self ! EndedMatch(matchDay.id, matchDay.date)
    }
  }
}
