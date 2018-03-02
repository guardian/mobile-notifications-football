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

  implicit class RichMatchDay(matchDay: MatchDay) {

    private lazy val internationalTeams = Set(
      "England",
      "Scotland",
      "Wales",
      "Rep of Ireland",
      "Spain",
      "Germany",
      "Italy",
      "France",
      "Belgium",
      "Portugal",
      "Turkey",
      "Poland",
      "Norway",
      "Sweden",
      "Denmark",
      "Russia",
      "Argentina",
      "Brazil"
    )

    def isUncoveredInternationalFriendly: Boolean = Set(matchDay.homeTeam.name, matchDay.awayTeam.name).intersect(internationalTeams).isEmpty

    def isEarlyQualifyingRound: Boolean = competitionRoundToInt.map( r => r < 3 ).getOrElse(true)

    private def competitionRoundToInt: Option[Int] = {
      try {
        Some(matchDay.round.roundNumber.toInt)
      } catch {
        case e: Exception => None
      }
    }
  }


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

    //Theres some stuff that PA don't provide data for and we want to supress alerts for these matches
    def paProvideAlerts(matchDay: MatchDay) : Boolean = {
      matchDay.competition.map {
        c => c.id match {
          //International friendly: Must involve one of a specific set of teams
          case "721" if matchDay.isUncoveredInternationalFriendly => false

          //FA cup qualifying rounds not covererd before round 3
          case "303" if matchDay.isEarlyQualifyingRound => false

          case _ => true
        }
      }.getOrElse(false) //Shouldn't ever happen
    }

    logger.info("Retrieving today's matches from PA")
    val matches = paClient.aroundToday
    matches.map(
      _.filter(inProgress)
       .filter(paProvideAlerts)
       .filterNot(isMidnight)
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
