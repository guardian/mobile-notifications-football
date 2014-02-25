package com.gu.mobile.notifications.football.helpers

import pa.{MatchDay, MatchDayTeam}
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

  implicit class RichMatchDayCompanion(companion: MatchDay.type) {
    def empty = MatchDay(
      "",
      new DateTime(),
      None,
      None,
      "",
      false,
      false,
      false,
      false,
      false,
      "",
      None,
      MatchDayTeam.empty,
      MatchDayTeam.empty,
      None,
      None,
      None
    )

    def forIdAndStatus(id: String, status: String) = empty.copy(id = id, matchStatus = status)
  }
}
