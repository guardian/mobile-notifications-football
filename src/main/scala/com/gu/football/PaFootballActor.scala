package com.gu.football

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.gu.Logging
import com.gu.football.models.{Goal, GoalContext, Score}
import com.gu.mobile.notifications.client._
import com.gu.mobile.notifications.football.lib.{GoalNotificationBuilder, PaMatchDayClient}
import com.gu.scanamo.syntax._
import com.gu.scanamo.{ScanamoAsync, Table}
import org.joda.time.DateTime
import pa.MatchDay
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object PaFootballActor {
  case class CachedValue[T](value: T, expiry: DateTime)

  sealed trait DistinctStatus
  case object Distinct extends DistinctStatus
  case object Duplicate extends DistinctStatus
  case object Unknown extends DistinctStatus
}

class PaFootballActor(
  paMatchDayClient: PaMatchDayClient,
  client: AmazonDynamoDBAsync,
  tableName: String,
  goalNotificationBuilder: GoalNotificationBuilder,
  notificationClient: ApiClient
) extends Logging {

  import PaFootballActor._

  val eventsTable = Table[MatchEventWithId](tableName)
  var processedEvents = Set.empty[String]
  var cachedMatches: Option[CachedValue[List[MatchDay]]] = None

  def start: Future[Set[String]] = {
    logger.info("Starting poll for new match events")

    val matchEventIds = for {
      matches <- todaysMatches
      liveMatches = matches.filter(isLive)
      _ = logger.info(s"Processing ${liveMatches.map(_.id)}")
      ids <- Future.traverse(liveMatches)(processMatch)
    } yield ids.flatten.toSet

    matchEventIds andThen {
      case Success(events) =>
        logger.info("Finished polling with success")
        processedEvents = events
      case Failure(e) =>
        logger.error(s"Finished polling with error ${e.getMessage}")
    }
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

  private def isLive(m: MatchDay): Boolean = {
    println(s"${m.id}: ${m.date}, result: ${m.result}, beforeNow: ${m.date.isBeforeNow}")
    println(s"Now: ${DateTime.now()}")
    m.date.isBefore(DateTime.now()) && !m.result
  }

  private def sendGoalAlert(matchDay: MatchDay, previousEvents: List[MatchEventWithId])(goal: Goal): Future[Unit] = {
    logger.info(s"Sending goal alert $goal")
    import matchDay._
    val goalsToDate = goal :: previousEvents.flatMap(Goal.fromEvent(homeTeam, awayTeam)(_))
    val score = Score.fromGoals(homeTeam, awayTeam, goalsToDate)
    val goalContext = GoalContext(homeTeam, awayTeam, matchDay.id, score)
    val payload = goalNotificationBuilder.build(goal, goalContext)
    val result = notificationClient.send(payload)
    result.onComplete {
      case Success(Left(error)) => logger.error(s"Error sending $goal - ${error.description}")
      case Success(Right(())) => logger.info(s"Goal alert $goal successfully sent")
      case Failure(f) => logger.error(s"Error sending $goal ${f.getMessage}")
    }
    result
      .map(_ => ())
      .recover({case _ => ()})
  }

  private def runOncePerEvent(matchDay: MatchDay, previousEvents: List[MatchEventWithId], event: MatchEventWithId): Future[Unit] = {
    logger.info(s"Found unique event $event")
    val goal = Goal.fromEvent(matchDay.homeTeam, matchDay.awayTeam)(event)
    val sentGoalAlert = goal.map(sendGoalAlert(matchDay, previousEvents))
    sentGoalAlert.getOrElse(Future.successful(()))
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
    eventsForMatch(matchDay.id).flatMap(processMatchEvents(matchDay))

  private def eventsForMatch(matchid: String): Future[List[MatchEventWithId]] =
    paMatchDayClient.matchEvents(matchid).map { matchEvents =>
      matchEvents.events.flatMap(MatchEventWithId.fromMatchEvent(matchid))
    } recover {
      case e =>
        logger.error(s"Failed to fetch events for match $matchid: ${e.getMessage}")
        List.empty
    }

  private def processMatchEvents(matchDay: MatchDay)(events: List[MatchEventWithId]): Future[List[String]] =
    Future.traverse(events)(processMatchEvent(matchDay, events)).map(_.flatten)

  private def processMatchEvent(matchDay: MatchDay, events: List[MatchEventWithId])(event: MatchEventWithId): Future[Option[String]] = {
    if (!processedEvents.contains(event.id)) {
      dynamoDistinctCheck(event).flatMap {
        case Distinct =>
          val previousEvents = events.takeWhile(_ != event)
          runOncePerEvent(matchDay, previousEvents, event).map(_ => Some(event.id))
        case Duplicate =>
          logger.warn(s"duplicate event ${event.id}")
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
