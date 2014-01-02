package com.gu.mobile.notifications.football.lib

import pa.{MatchDayTeam, MatchDay}

object Pa {
  /** Extracts scorer name and minute of goal from scorer string (this is the format PA gives us) */
  val ScorerMatcher = """^(.+)\s+\((\d+)\)$""".r

  implicit class RichMatchDay(matchDay: MatchDay) {
    def summaryString: String =
      s"${matchDay.homeTeam.name} vs ${matchDay.awayTeam.name} at " +
        s"${matchDay.date.hourOfDay.get()}:${matchDay.date.minuteOfHour.get()}"

    def goals: Seq[Goal] = (matchDay.homeTeam.goals ++ matchDay.awayTeam.goals).sortBy(_.minute)
  }

  implicit class RichMatchDayTeam(matchDayTeam: MatchDayTeam) {
    def goals: Seq[Goal] = {
      matchDayTeam.scorers.getOrElse("").split(",\\s*") collect {
        case ScorerMatcher(name, IntegerString(minute)) => Goal(minute, name, matchDayTeam)
      }
    }
  }

  case class Goal(minute: Int, scorerName: String, scoringTeam: MatchDayTeam)

  def delta(matchDay1: MatchDay, matchDay2: MatchDay): Set[Goal] = matchDay2.goals.toSet diff matchDay1.goals.toSet
}
