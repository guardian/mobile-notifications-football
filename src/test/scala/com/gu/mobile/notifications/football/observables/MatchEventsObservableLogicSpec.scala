package com.gu.mobile.notifications.football.observables

import org.scalatest.{WordSpec, Matchers}
import com.gu.mobile.notifications.football.lib.ResourcesHelper
import pa.Parser.parseMatchEvents
import rx.lang.scala.Observable
import com.gu.mobile.notifications.football.models._

class MatchEventsObservableLogicSpec extends WordSpec with Matchers with ResourcesHelper {
  val homeTeam = MatchEventTeam(2, "Aston Villa")
  val awayTeam = MatchEventTeam(14, "Norwich")

  "eventsFromFeeds" should {
    "correctly reproduce the sequence of events without duplicates" in {
      val Some(finalFixture) = parseMatchEvents(slurpOrDie("pa/football/match/events/key/3704151.xml"))

      val fixture1 = finalFixture.copy(isResult = false, events = finalFixture.events.take(10))
      val fixture2 = finalFixture.copy(isResult = false, events = finalFixture.events.take(20))
      val fixture3 = finalFixture.copy(isResult = false, events = finalFixture.events.take(30))

      val matchEventsSequence = Observable.items(fixture1, fixture1, fixture2, fixture3, fixture3, finalFixture)

      MatchEventsObservableLogic
        .eventsFromFeeds("3704151", matchEventsSequence)
        .toBlockingObservable.toList.map(_._1) should be(
        List(
          KickOff,
          Goal("Wes Hoolahan", awayTeam, homeTeam, 3),
          Goal("Christian Benteke", homeTeam, awayTeam, 25),
          Goal("Christian Benteke", homeTeam, awayTeam, 27),
          Goal("Leandro Bacuna", homeTeam, awayTeam, 37),
          OwnGoal("Sebastien Bassong", homeTeam, awayTeam, 41),
          Result
        )
      )
    }
  }
}
