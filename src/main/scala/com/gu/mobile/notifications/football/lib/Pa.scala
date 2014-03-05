package com.gu.mobile.notifications.football.lib

import pa.{MatchEvent, MatchDay}
import org.joda.time.Duration
import org.joda.time.format.PeriodFormatter

object Pa {
  val MatchEndedStatuses = List(
    "FT",
    "PTFT",
    "Result",
    "ETFT",
    "MC",
    "Abandoned",
    "Cancelled"
  )

  implicit class RichMatchDay(matchDay: MatchDay) {
    def summaryString: String =
      s"${matchDay.homeTeam.name} vs ${matchDay.awayTeam.name} at " +
        s"${matchDay.date.hourOfDay.get()}:${matchDay.date.minuteOfHour.get()} (${matchDay.matchStatus})"

    def hasEnded: Boolean = MatchEndedStatuses contains matchDay.matchStatus
  }
}
