package com.gu.mobile.notifications.football.lib

import com.gu.Logging
import com.gu.mobile.notifications.football.models.{MatchData, MatchId}
import org.joda.time.DateTime
import pa.{MatchDay, MatchEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

case class EndedMatch(matchId: String, startTime: DateTime)

class FootballData(
  paClient: PaFootballClient,
  eventFilter: EventFilter,
  syntheticEvents: SyntheticMatchEventGenerator,
  eventConsumer: EventConsumer
) extends Logging {

  private var endedMatches: Set[EndedMatch] = Set.empty
  private val cachedMatches = new CachedValue[List[MatchDay]](30.minutes)

  def pollFootballData: Future[List[MatchData]] = {
    logger.info("Starting poll for new match events")

    val matchesData = for {
      liveMatches <- matchIdsInProgress
      md <- Future.traverse(liveMatches)(processMatch)
    } yield md.flatten

    matchesData andThen {
      case Success(data) =>
        logger.info(s"Finished polling with success, fetched ${data.size} matches' data")
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
      eventsToProcess <- eventFilter.filterProcessedEvents(matchDay.id)(events)
    } yield Some(MatchData(matchDay, events, eventsToProcess))

    matchData.recover { case NonFatal(exception) =>
      logger.error(s"Failed to process match ${matchId.id}: ${exception.getMessage}", exception)
      None
    }
  }

  private def handleMatchEnd(matchDay: MatchDay, event: MatchEvent) = {
    if (event.eventType == "full-time") {
      endedMatches = endedMatches.filter(_.startTime.isAfter(DateTime.now.minusDays(2))) + EndedMatch(matchDay.id, matchDay.date)
    }
  }
}
