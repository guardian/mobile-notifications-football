package com.gu.mobile.notifications.football.lib

import com.gu.Logging
import com.gu.mobile.notifications.football.lib.DynamoDistinctCheck._
import com.gu.mobile.notifications.football.models.{FilteredMatchData, RawMatchData}
import pa.MatchEvent

import scala.concurrent.{ExecutionContext, Future}

class EventFilter(distinctCheck: DynamoDistinctCheck) extends Logging {
  private var processedEvents = Set.empty[String]

  private def filterOneEvent(matchId: String)(event: MatchEvent)(implicit ec: ExecutionContext): Future[Option[MatchEvent]] = {
    event.id.filterNot(processedEvents.contains).map { eventId =>
      distinctCheck.insertEvent(matchId, eventId, event).map {
        case Distinct =>
          processedEvents = processedEvents + eventId
          Some(event)
        case Duplicate =>
          processedEvents = processedEvents + eventId
          None
        case _ => None
      }
    } getOrElse {
      logger.debug(s"Event ${event.id} already exists in local cache or does not have an id - discarding")
      Future.successful(None)
    }
  }

  def filterRawMatchData(matchData: RawMatchData)(implicit ec: ExecutionContext): Future[FilteredMatchData] = {
    val filteredEvents = Future.traverse(matchData.allEvents)(filterOneEvent(matchData.matchDay.id)).map(_.flatten)
    filteredEvents.map(matchData.withFilteredEvents)
  }

  def filterRawMatchDataList(matches: List[RawMatchData])(implicit ec: ExecutionContext): Future[List[FilteredMatchData]] = {
    Future.traverse(matches)(filterRawMatchData)
      .map(_.filter(_.filteredEvents.nonEmpty))
  }
}
