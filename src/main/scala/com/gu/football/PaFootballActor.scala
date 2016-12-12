package com.gu.football

import akka.actor.{Actor, ActorRef}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.gu.mobile.notifications.football.lib.PaMatchDayClient
import com.gu.scanamo.syntax._
import com.gu.scanamo.{ScanamoAsync, Table}
import grizzled.slf4j.Logging
import org.joda.time.DateTime
import pa.MatchDay

import scala.concurrent.Future
import scala.util.{Failure, Success}

object PaFootballActor {
  case class CachedValue[T](value: T, expiry: DateTime)
  case object TriggerPoll
  case class PollFinished(senderRef: ActorRef)
  case class ProcessedEvents(events: Set[String], senderRef: ActorRef)

  sealed trait DistinctStatus
  case object Distinct extends DistinctStatus
  case object Duplicate extends DistinctStatus
  case object Unknown extends DistinctStatus
}

class PaFootballActor(paMatchDayClient: PaMatchDayClient, client: AmazonDynamoDBAsync, tableName: String) extends Actor with Logging {

  implicit val ec = context.dispatcher

  import PaFootballActor._

  val eventsTable = Table[MatchEventWithId](tableName)
  var processedEvents = Set.empty[String]
  var ready: Boolean = true
  var cachedMatches: Option[CachedValue[List[MatchDay]]] = None

  def receive = {
    case TriggerPoll if !ready =>
      logger.warn("Poll triggered while existing poll still in progress, ignoring")

    case TriggerPoll if ready =>
      implicit val lg = logger
      val senderRef = sender()
      ready = false
      logger.info("Starting poll for new match events")

      val matchEventIds = for {
        matches <- todaysMatches
        liveMatches = matches.filter(isLive)
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
  }


  def dynamoDistinctCheck(event: MatchEventWithId): Future[DistinctStatus] = {
    val putResult = ScanamoAsync.exec(client)(eventsTable.given(not(attributeExists('id))).put(event))
    putResult map {
      case Right(s) =>
        logger.debug(s"Distinct event ${event.id} written to dynamodb")
        Distinct
      case Left(s) =>
        logger.debug(s"Event ${event.id} already exists in dynamodb - discarding")
        Duplicate
    } recover {
      case e =>
        logger.error(s"Failure while writing to dynamodb: ${e.getMessage}.  Request will be retried on next poll")
        Unknown
    }
  }

  private def isLive(m: MatchDay): Boolean =
    m.date.isBefore(DateTime.now) && !m.result

  private def runOncePerEvent(event: MatchEventWithId): Future[Unit] = {
    logger.info(s"Found unique event $event")
    Future.successful(())
  }

  private def todaysMatches: Future[List[MatchDay]] = {
    val matches = for {
      matches <- cachedMatches if matches.expiry isAfter DateTime.now
    } yield {
      logger.info(s"Using cached value of today's matches (expires at ${matches.expiry})")
      Future.successful(matches.value)
    }

    matches.getOrElse {
      logger.info("Retrieving today's matches from PA")
      paMatchDayClient.aroundToday.andThen {
        case Success(m) =>
          logger.info(s"Retrieved ${m.size} matches from PA")
          cachedMatches = Some(CachedValue(m, DateTime.now.plusMinutes(30)))

        case Failure(e) =>
          logger.error(s"Failed to retrieve today's matches from PA: ${e.getMessage}")
      } recover {
        case e =>
          List.empty
      }
    }
  }
  private def processMatch(matchDay: MatchDay): Future[List[String]] =
    eventsForMatch(matchDay.id).flatMap(processMatchEvents)

  private def eventsForMatch(id: String): Future[List[MatchEventWithId]] =
    paMatchDayClient.matchEvents(id).map { matchEvents =>
      matchEvents.events.flatMap(MatchEventWithId.fromMatchEvent)
    } recover {
      case e =>
        logger.error(s"Failed to fetch events for match $id: ${e.getMessage}")
        List.empty
    }

  private def processMatchEvents(events: List[MatchEventWithId]): Future[List[String]] =
    Future.traverse(events)(processMatchEvent).map(_.flatten)

  private def processMatchEvent(event: MatchEventWithId): Future[Option[String]] = {
    if (!processedEvents.contains(event.id)) {
      dynamoDistinctCheck(event).flatMap {
        case Distinct =>
          runOncePerEvent(event).map(_ => Some(event.id))
        case Duplicate =>
          Future.successful(Some(event.id))
        case Unknown =>
          Future.successful(None)
      }
    } else {
      logger.debug(s"Event ${event.id} already exists in local cache - discarding")
      Future.successful(Some(event.id))
    }
  }
}
