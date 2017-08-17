package com.gu.mobile.notifications.football

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import org.joda.time.DateTime
import com.gu.mobile.notifications.football.lib.{CachedValue, DynamoDistinctCheck, PaFootballClient, SyntheticMatchEventGenerator}
import DynamoDistinctCheck.Distinct
import com.gu.Logging
import com.gu.mobile.notifications.football.models.{MatchData, MatchId}
import pa.{MatchDay, MatchEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal

case class EndedMatch(matchId: String, startTime: DateTime)

class FootballData(
  paClient: PaFootballClient,
  distinctCheck: DynamoDistinctCheck,
  syntheticEvents: SyntheticMatchEventGenerator,
  eventConsumer: EventConsumer
) extends Logging {

  //private var processedEvents = Set.empty[String]
  // TODO re-instate a local cache, outside of this object as it's a side effect
  private var endedMatches: Set[EndedMatch] = Set.empty
  private val cachedMatches = new CachedValue[List[MatchDay]](30.minutes)

  def pollFootballData: Future[List[MatchData]] = {
    logger.info("Starting poll for new match events")

    val matchesData = for {
      liveMatches <- matchIdsInProgress
      notifications <- Future.traverse(liveMatches)(processMatch)
    } yield notifications.flatten

    matchesData andThen {
      case Success(notifications) =>
        //processedEvents = ids
        logger.info(s"Finished polling with success, created ${notifications.size} notifications")
      case Failure(e) =>
        logger.error(s"Finished polling with error ${e.getMessage}")
    }
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

  private def processMatch(matchId: MatchId): Future[Option[MatchData]] = {
    val matchData = for {
      (matchDay, events) <- paClient.eventsForMatch(matchId, syntheticEvents)
      eventsToProcess <- Future.traverse(events)(processMatchEvent(matchDay, events))
    } yield Some(MatchData(matchDay, events, eventsToProcess.flatten))

    matchData.recover { case NonFatal(exception) =>
      logger.error(s"Failed to process match ${matchId.id}: ${exception.getMessage}", exception)
      None
    }
  }

  private def processMatchEvent(matchDay: MatchDay, events: List[MatchEvent])(event: MatchEvent): Future[Option[MatchEvent]] = {
    handleMatchEnd(matchDay, event)

    event.id.map { eventId =>
      distinctCheck.insertEvent(matchDay.id, eventId, event).map {
        case Distinct => Some(event)
        case _ => None
      }
    } getOrElse {
      logger.debug(s"Event ${event.id} already exists in local cache or does not have an id - discarding")
      Future.successful(None)
    }
  }

  private def handleMatchEnd(matchDay: MatchDay, event: MatchEvent) = {
    if (event.eventType == "full-time") {
      endedMatches = endedMatches.filter(_.startTime.isAfter(DateTime.now.minusDays(2))) + EndedMatch(matchDay.id, matchDay.date)
    }
  }
}
