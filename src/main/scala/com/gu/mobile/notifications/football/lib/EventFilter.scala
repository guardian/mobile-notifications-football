package com.gu.mobile.notifications.football.lib

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator

import com.gu.Logging
import com.gu.mobile.notifications.football.lib.DynamoDistinctCheck._
import com.gu.mobile.notifications.football.models.{FilteredMatchData, RawMatchData}
import pa.MatchEvent

import scala.concurrent.{ExecutionContext, Future}

class EventFilter(distinctCheck: DynamoDistinctCheck) extends Logging {
  private val processedEvents = new AtomicReference[Set[String]](Set.empty)

  private def cache(eventId: String): Unit = {
    processedEvents.getAndUpdate(new UnaryOperator[Set[String]] {
      override def apply(set: Set[String]): Set[String] = set + eventId
    })
  }

  private def filterOneEvent(matchId: String)(event: MatchEvent)(implicit ec: ExecutionContext): Future[Option[MatchEvent]] = {
    event.id.filterNot(processedEvents.get.contains).map { eventId =>
      distinctCheck.insertEvent(matchId, eventId, event).map {
        case Distinct =>
          cache(eventId)
          Some(event)
        case Duplicate =>
          cache(eventId)
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
