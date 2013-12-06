package com.gu.mobile.notifications.football.lib

import pa.MatchDay

object Pa {
  implicit class RichMatchDay(matchDay: MatchDay) {
    def summaryString: String =
      s"${matchDay.homeTeam.name} vs ${matchDay.awayTeam.name} at " +
        s"${matchDay.date.hourOfDay.get()}:${matchDay.date.minuteOfHour.get()}"
  }
}
