package com.gu.mobile.notifications.football.models

import pa.{MatchDay, MatchEvent}

case class MatchData(
  matchDay: MatchDay,
  allEvents: List[MatchEvent],
  eventsToProcess: List[MatchEvent]
)
