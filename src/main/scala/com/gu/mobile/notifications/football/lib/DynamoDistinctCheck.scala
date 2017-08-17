package com.gu.mobile.notifications.football.lib

import scala.concurrent.{ExecutionContext, Future}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.gu.Logging
import com.gu.scanamo.{ScanamoAsync, Table}
import com.gu.scanamo.syntax._
import pa.MatchEvent

case class DynamoMatchEvent(
  eventId: String,
  matchId: String,
  matchEvent: MatchEvent,
  ttl: Long
)

object DynamoMatchEvent {
  def apply(matchId: String, eventId: String, event: MatchEvent): DynamoMatchEvent = DynamoMatchEvent(
    eventId = eventId,
    matchId = matchId,
    matchEvent = event,
    ttl = (System.currentTimeMillis() / 1000) + (14 * 24 * 3600)
  )
}

object DynamoDistinctCheck {
  sealed trait DistinctStatus
  case object Distinct extends DistinctStatus
  case object Duplicate extends DistinctStatus
  case object Unknown extends DistinctStatus
}

class DynamoDistinctCheck(client: AmazonDynamoDBAsync, tableName: String) extends Logging {
  import DynamoDistinctCheck._

  val eventsTable = Table[DynamoMatchEvent](tableName)

  def insertEvent(matchId: String, eventId: String, event: MatchEvent)(implicit ec: ExecutionContext): Future[DistinctStatus] = {
    val putResult = ScanamoAsync.exec(client)(eventsTable.given(not(attributeExists('id))).put(DynamoMatchEvent(matchId, eventId, event)))
    putResult map {
      case Right(_) =>
        logger.info(s"Distinct event ${event.id} written to dynamodb")
        Distinct
      case Left(_) =>
        logger.debug(s"Event ${event.id} already exists in dynamodb - discarding")
        Duplicate
    } recover {
      case e =>
        logger.error(s"Failure while writing to dynamodb: ${e.getMessage}.  Request will be retried on next poll")
        Unknown
    }
  }
}
