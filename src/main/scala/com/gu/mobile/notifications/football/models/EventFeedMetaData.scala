package com.gu.mobile.notifications.football.models


object EventFeedMetaData {
  def incrementHomeScore(metadata: EventFeedMetaData) = {
    metadata.copy(homeTeamScore = metadata.homeTeamScore + 1)
  }

  def incrementAwayScore(metadata: EventFeedMetaData) = {
    metadata.copy(awayTeamScore = metadata.awayTeamScore + 1)
  }

  implicit class RichEventsList(events: Seq[MatchEvent]) {
    /** Given a list of events, zips each event with the current state of the EventFeedMetaData */
    def zipWithMetadata(
      matchID: String, homeTeam: MatchEventTeam, awayTeam: MatchEventTeam
    ): Seq[(MatchEvent, EventFeedMetaData)] = {
      val initialMetaData = EventFeedMetaData(
        matchID,
        homeTeam,
        0,
        awayTeam,
        0
      )

      val metas = events.scanLeft(initialMetaData) { (metaData, event) =>
        val transform: EventFeedMetaData => EventFeedMetaData = event match {
          case Goal(_, scoringTeam, _) if scoringTeam == metaData.homeTeam =>
            incrementHomeScore
          case Goal(_, scoringTeam, _) if scoringTeam == metaData.awayTeam =>
            incrementAwayScore
          case OwnGoal(_, ownScoringTeam, _) if ownScoringTeam == metaData.homeTeam =>
            incrementAwayScore
          case OwnGoal(_, ownScoringTeam, _) if ownScoringTeam == metaData.awayTeam =>
            incrementHomeScore
          case _ => identity
        }

        transform(metaData)
      }

      events zip metas.drop(1)
    }
  }

}

/** Stores additional information essential for notifications, including the match ID, what the home and away team are,
  * and the current score after the event occurs
  */
case class EventFeedMetaData(
    matchID: String,
    homeTeam: MatchEventTeam,
    homeTeamScore: Int,
    awayTeam: MatchEventTeam,
    awayTeamScore: Int
)