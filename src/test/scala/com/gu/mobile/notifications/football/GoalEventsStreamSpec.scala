package com.gu.mobile.notifications.football

import org.scalatest.{Matchers, WordSpec}
import pa.{MatchDayTeam, MatchDay}
import org.joda.time.DateTime
import rx.lang.scala.Observable
import com.gu.mobile.notifications.football.lib.GoalEvent
import com.gu.mobile.notifications.football.lib.Pa.Goal

class GoalEventsStreamSpec extends WordSpec with Matchers {
  val matchDay1 = MatchDay(
    "test",
    new DateTime(),
    None,
    None,
    "leg",
    true,
    false,
    false,
    false,
    false,
    "LIVE",
    None,
    MatchDayTeam("manchester-united", "Manchester United", Some(0), None, None, None),
    MatchDayTeam("bolton-wanderers", "Bolton Wanderers", Some(0), None, None, None),
    None,
    None,
    None
  )

  val matchDay2 = matchDay1.copy(
    homeTeam=MatchDayTeam("manchester-united", "Manchester United", Some(1), None, None, Some("David Beckham (10)"))
  )

  val matchDays = Observable(List(matchDay1), List(matchDay2))

  "Streams.goalEvents" when {
    "given a stream of two MatchDays over which a goal has been scored" should {
      "produce a stream containing an event for the goal that was scored" in {
        val goalEvents = Streams.goalEvents(matchDays).toBlockingObservable.toList

        goalEvents should have length 1

        goalEvents(0) shouldEqual GoalEvent(Goal(10, "David Beckham", matchDay2.homeTeam), matchDay2)
      }
    }
  }
}
