package com.gu.mobile.notifications.football.lib

import com.gu.Logging
import com.gu.mobile.notifications.football.lib.DynamoDistinctCheck._
import pa.MatchEvent

import scala.concurrent.{ExecutionContext, Future}

trait EventFilter {
  def filterProcessedEvents(matchId: String)(events: List[MatchEvent])(implicit ec: ExecutionContext): Future[List[MatchEvent]]
}

class CachedEventFilter(distinctCheck: DynamoDistinctCheck) extends EventFilter with Logging {
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

  override def filterProcessedEvents(matchId: String)(events: List[MatchEvent])(implicit ec: ExecutionContext): Future[List[MatchEvent]] = {
    Future.traverse(events)(filterOneEvent(matchId)).map(_.flatten)
  }
}
