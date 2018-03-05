package com.gu.mobile.notifications.football.lib

import com.gu.Logging
import com.gu.mobile.notifications.football.models.RawMatchData
import org.joda.time.DateTime
import pa.MatchDay

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

case class EndedMatch(matchId: String, startTime: DateTime)

class FootballData(
  paClient: PaFootballClient,
  syntheticEvents: SyntheticMatchEventGenerator
) extends Logging {

  def pollFootballData: Future[List[RawMatchData]] = {
    logger.info("Starting poll for new match events")

    val matchesData = for {
      liveMatches <- matchIdsInProgress
      md <- Batch.process(liveMatches, 5)(processMatch)
    } yield md.flatten

    matchesData andThen {
      case Success(data) =>
        logger.info(s"Finished polling with success, fetched ${data.size} matches' data")
      case Failure(e) =>
        logger.error(s"Finished polling with error ${e.getMessage}")
    }
  }

  private def matchIdsInProgress: Future[List[MatchDay]] = {
    def inProgress(m: MatchDay): Boolean =
      m.date.minusMinutes(5).isBeforeNow && m.date.plusHours(4).isAfterNow

    // unfortunately PA provide 00:00 as start date when they don't have the start date
    // so we can't do anything with these matches
    def isMidnight(matchDay: MatchDay): Boolean = {
      val localDate = matchDay.date.toLocalTime
      localDate.getHourOfDay == 0 && localDate.getMinuteOfHour == 0
    }

    logger.info("Retrieving today's matches from PA")
    val matches = paClient.aroundToday
    matches.map(
      _.filter(inProgress).filterNot(isMidnight)
    )
  }

  private def processMatch(matchDay: MatchDay): Future[Option[RawMatchData]] = {
    val matchData = for {
      (matchDay, events) <- paClient.eventsForMatch(matchDay, syntheticEvents)
    } yield Some(RawMatchData(matchDay, events))

    matchData.recover { case NonFatal(exception) =>
      logger.error(s"Failed to process match ${matchDay.id}: ${exception.getMessage}", exception)
      None
    }
  }

}
