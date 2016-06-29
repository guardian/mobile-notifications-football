package com.gu.mobile.notifications.football.helpers

import pa.{Round, Stage, MatchEvent, MatchDay, MatchDayTeam}
import org.joda.time.DateTime

trait EmptyInstances {
  implicit class RichMatchDayTeamCompanion(companion: MatchDayTeam.type) {
    def empty = MatchDayTeam(
      "",
      "",
      None,
      None,
      None,
      None
    )
  }

  object PaMatchEvent {
    def empty = new MatchEvent(
      None,
      None,
      "",
      None,
      None,
      None,
      Nil,
      None,
      None,
      None,
      None,
      None,
      None
    )
  }

  implicit class RichMatchDayCompanion(companion: MatchDay.type) {
    def empty = MatchDay(
      id = "",
      date = new DateTime(),
      competition = None,
      stage = Stage("1"),
      round = Round("12", None),
      leg = "",
      liveMatch = false,
      result = false,
      previewAvailable = false,
      reportAvailable = false,
      lineupsAvailable = false,
      matchStatus = "F",
      attendance = None,
      homeTeam = MatchDayTeam.empty,
      awayTeam = MatchDayTeam.empty,
      referee = None,
      venue = None,
      comments = None
    )

    def forIdAndStatus(id: String, status: String) = empty.copy(id = id, matchStatus = status)
  }
}
