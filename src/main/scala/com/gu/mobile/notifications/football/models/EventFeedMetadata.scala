package com.gu.mobile.notifications.football.models

object EventFeedMetadata {
  def incrementHomeScore(metadata: EventFeedMetadata) = {
    metadata.copy(homeTeamScore = metadata.homeTeamScore + 1)
  }

  def incrementAwayScore(metadata: EventFeedMetadata) = {
    metadata.copy(awayTeamScore = metadata.awayTeamScore + 1)
  }

  implicit class RichEventsList(events: Seq[MatchEvent]) {
    /** Given a list of events, zips each event with the current state of the EventFeedMetaData */
    def zipWithMetadata(
      matchID: String, homeTeam: MatchEventTeam, awayTeam: MatchEventTeam
    ): Seq[(MatchEvent, EventFeedMetadata)] = {
      val initialMetaData = EventFeedMetadata(
        matchID = matchID,
        homeTeam = homeTeam,
        homeTeamScore = 0,
        awayTeam = awayTeam,
        awayTeamScore = 0
      )

      val metas = events.scanLeft(initialMetaData) { (metaData, event) =>
        val transform: EventFeedMetadata => EventFeedMetadata = event match {
          case event: ScoreEvent if event.scoringTeam == metaData.homeTeam =>
            incrementHomeScore
          case event: ScoreEvent if event.scoringTeam == metaData.awayTeam =>
            incrementAwayScore
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
case class EventFeedMetadata(
    matchID: String,
    homeTeam: MatchEventTeam,
    homeTeamScore: Int,
    awayTeam: MatchEventTeam,
    awayTeamScore: Int
)