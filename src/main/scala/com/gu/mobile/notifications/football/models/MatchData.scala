package com.gu.mobile.notifications.football.models

import pa.{MatchDay, MatchEvent}

case class RawMatchData(
  matchDay: MatchDay,
  allEvents: List[MatchEvent]
) {
  def withFilteredEvents(filteredEvents: List[MatchEvent]): FilteredMatchData = FilteredMatchData(
    matchDay = matchDay,
    allEvents = allEvents,
    filteredEvents = filteredEvents
  )
}

case class FilteredMatchData(
  matchDay: MatchDay,
  allEvents: List[MatchEvent],
  filteredEvents: List[MatchEvent]
) {
  def withArticle(articleId: Option[String]): MatchDataWithArticle = MatchDataWithArticle(
    matchDay = matchDay,
    allEvents = allEvents,
    filteredEvents = filteredEvents,
    articleId = articleId
  )
}

case class MatchDataWithArticle(
  matchDay: MatchDay,
  allEvents: List[MatchEvent],
  filteredEvents: List[MatchEvent],
  articleId: Option[String]
)
