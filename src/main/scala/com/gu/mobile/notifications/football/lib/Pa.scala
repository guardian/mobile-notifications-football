package com.gu.mobile.notifications.football.lib

import pa.{MatchDayTeam, MatchDay}
import rx.lang.scala.Observable
import Observables._
import com.gu.mobile.notifications.football.lib.Pa.Goal

case class GoalEvent(goal: Goal, matchDay: MatchDay)

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

  /** Given a stream of lists of MatchDay results, returns a stream of goal events */
  def goalNotificationStream(matchDays: Observable[List[MatchDay]]): Observable[GoalEvent] = {
    matchDays.pairs flatMap { case (oldMatchDays, newMatchDays) =>
      Observable((Lists.pairs(oldMatchDays, newMatchDays)(_.id) flatMap { case (oldMatchDay, newMatchDay) =>
        delta(oldMatchDay, newMatchDay) map { goal => GoalEvent(goal, newMatchDay) }
      }).toList: _*)
    }
  }
}
