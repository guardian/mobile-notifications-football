package com.gu.football

import akka.actor.{Actor, ActorRef}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.amazonaws.services.lambda.runtime.LambdaLogger
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
  case class TriggerPoll(logger: LambdaLogger)
  case class PollFinished(logger: LambdaLogger, senderRef: ActorRef)
  case class ProcessedEvents(events: Set[String], logger: LambdaLogger, senderRef: ActorRef)

  sealed trait DistinctStatus
  case object Distinct extends DistinctStatus
  case object Duplicate extends DistinctStatus
  case object Unknown extends DistinctStatus
}

object Logger {
  def info(s: => String)(implicit logger: LambdaLogger): Unit =
    logger.log(s"$s\n")

  def warn(s: => String)(implicit logger: LambdaLogger): Unit =
    logger.log(s"$s\n")

  def error(s: => String)(implicit logger: LambdaLogger): Unit =
    logger.log(s"$s\n")

  def debug(s: => String)(implicit logger: LambdaLogger): Unit = {}
}

class PaFootballActor(paMatchDayClient: PaMatchDayClient, client: AmazonDynamoDBAsync, tableName: String) extends Actor {

  implicit val ec = context.dispatcher

  import PaFootballActor._

  val eventsTable = Table[MatchEventWithId](tableName)
  var processedEvents = Set.empty[String]
  var ready: Boolean = true
  var cachedMatches: Option[CachedValue[List[MatchDay]]] = None

  def receive = {
    case TriggerPoll(logger) if !ready =>
      Logger.warn("Poll triggered while existing poll still in progress, ignoring")(logger)

    case TriggerPoll(logger) if ready =>
      implicit val lg = logger
      val senderRef = sender()
      ready = false
      Logger.info("Starting poll for new match events")

      val matchEventIds = for {
        matches <- todaysMatches
        liveMatches = matches.filter(isLive)
        ids <- Future.traverse(liveMatches)(processMatch)
      } yield ids.flatten.toSet

      matchEventIds andThen {
        case Success(ids) =>
          Logger.info("Finished polling with success")
          self ! ProcessedEvents(ids, logger, senderRef)
        case Failure(e) =>
          Logger.error(s"Finished polling with error ${e.getMessage}")
          self ! PollFinished(logger, senderRef)
      }

    case ProcessedEvents(events, logger, senderRef) =>
      processedEvents = events
      self ! PollFinished(logger, senderRef)

    case PollFinished(logger, senderRef) =>
      ready = true
      Logger.info("Stopping lambda")(logger)
      senderRef ! "done"
  }


  def dynamoDistinctCheck(event: MatchEventWithId)(implicit logger: LambdaLogger): Future[DistinctStatus] = {
    val putResult = ScanamoAsync.exec(client)(eventsTable.given(not(attributeExists('id))).put(event))
    putResult map {
      case Right(s) =>
        Logger.debug(s"Distinct event ${event.id} written to dynamodb")
        Distinct
      case Left(s) =>
        Logger.debug(s"Event ${event.id} already exists in dynamodb - discarding")
        Duplicate
    } recover {
      case e =>
        Logger.error(s"Failure while writing to dynamodb: ${e.getMessage}.  Request will be retried on next poll")
        Unknown
    }
  }

  private def isLive(m: MatchDay): Boolean =
    m.date.isBefore(DateTime.now) // && !m.result

  private def runOncePerEvent(event: MatchEventWithId)(implicit logger: LambdaLogger): Future[Unit] = {
    Logger.info(s"Found unique event $event")
    Future.successful(())
  }

  private def todaysMatches(implicit logger: LambdaLogger): Future[List[MatchDay]] = {
    val matches = for {
      matches <- cachedMatches if matches.expiry isAfter DateTime.now
    } yield {
      Logger.info(s"Using cached value of today's matches (expires at ${matches.expiry})")
      Future.successful(matches.value)
    }

    matches.getOrElse {
      Logger.info("Retrieving today's matches from PA")
      paMatchDayClient.aroundToday.andThen {
        case Success(m) =>
          Logger.info(s"Retrieved ${m.size} matches from PA")
          cachedMatches = Some(CachedValue(m, DateTime.now.plusMinutes(30)))

        case Failure(e) =>
          Logger.error(s"Failed to retrieve today's matches from PA: ${e.getMessage}")
      } recover {
        case e =>
          List.empty
      }
    }
  }
  private def processMatch(matchDay: MatchDay)(implicit logger: LambdaLogger): Future[List[String]] =
    eventsForMatch(matchDay.id).flatMap(processMatchEvents)

  private def eventsForMatch(id: String)(implicit logger: LambdaLogger): Future[List[MatchEventWithId]] =
    paMatchDayClient.matchEvents(id).map { matchEvents =>
      matchEvents.events.flatMap(MatchEventWithId.fromMatchEvent)
    } recover {
      case e =>
        Logger.error(s"Failed to fetch events for match $id: ${e.getMessage}")
        List.empty
    }

  private def processMatchEvents(events: List[MatchEventWithId])(implicit logger: LambdaLogger): Future[List[String]] =
    Future.traverse(events)(processMatchEvent).map(_.flatten)

  private def processMatchEvent(event: MatchEventWithId)(implicit logger: LambdaLogger): Future[Option[String]] = {
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
      Logger.debug(s"Event ${event.id} already exists in local cache - discarding")
      Future.successful(Some(event.id))
    }
  }
}
