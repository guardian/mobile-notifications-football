package com.gu.football

import pa.{MatchEvent, Player}

case class MatchEventWithId(
  id: String,
  matchId: String,
  teamID: Option[String],
  eventType: String,
  matchTime: Option[String],
  eventTime: Option[String],
  addedTime: Option[String],
  players: List[Player],
  reason: Option[String],
  how: Option[String],
  whereFrom: Option[String],
  whereTo: Option[String],
  distance: Option[String],
  outcome: Option[String],
  expiry: Long
)

object MatchEventWithId {
  def fromMatchEvent(matchId: String)(event: MatchEvent): Option[MatchEventWithId] = event.id map { id =>
    MatchEventWithId(
      id,
      matchId,
      event.teamID,
      event.eventType,
      event.matchTime,
      event.eventTime,
      event.addedTime,
      event.players,
      event.reason,
      event.how,
      event.whereFrom,
      event.whereTo,
      event.distance,
      event.outcome,
      (System.currentTimeMillis() / 1000) + (14 * 24 * 3600)
    )
  }
}