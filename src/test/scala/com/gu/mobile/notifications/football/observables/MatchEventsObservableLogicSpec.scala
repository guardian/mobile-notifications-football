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

      val fixture1 = finalFixture.copy(isResult = false, events = finalFixture.events.take(20))
      val fixture2 = finalFixture.copy(isResult = false, events = finalFixture.events.take(40))
      val fixture3 = finalFixture.copy(isResult = false, events = finalFixture.events.take(60))

      val matchEventsSequence = Observable.items(fixture1, fixture1, fixture2, fixture3, fixture3, finalFixture)

      MatchEventsObservableLogic
        .eventsFromFeeds("3704151", matchEventsSequence)
        .toBlockingObservable.toList.map(_._1) should be(
        List(
          KickOff,
          Goal("Wes Hoolahan", awayTeam, homeTeam, 3, None),
          Goal("Christian Benteke", homeTeam, awayTeam, 25, None),
          Goal("Christian Benteke", homeTeam, awayTeam, 27, None),
          Goal("Leandro Bacuna", homeTeam, awayTeam, 37, None),
          OwnGoal("Sebastien Bassong", homeTeam, awayTeam, 41, None),
          Result
        )
      )
    }

    "correctly reproduce another series of events without duplicates" in {
      val Some(finalFixture) = parseMatchEvents(slurpOrDie("pa/football/match/events/key/3693727.xml"))

      val fixture1 = finalFixture.copy(isResult = false, events = finalFixture.events.take(25))
      val fixture2 = finalFixture.copy(isResult = false, events = finalFixture.events.take(50))
      val fixture3 = finalFixture.copy(isResult = false, events = finalFixture.events.take(80))

      val matchEventsSequence = Observable.items(fixture1, fixture1, fixture2, fixture3, fixture3, finalFixture)

      MatchEventsObservableLogic
        .eventsFromFeeds("3693727", matchEventsSequence)
        .toBlockingObservable.toList.map(_._1) should be(
        List(
          KickOff,
          Goal(
            "Jonathan de Guzman",
            MatchEventTeam(65, "Swansea"),
            MatchEventTeam(5, "Crystal Palace"),
            25,
            None
          ),
          PenaltyGoal(
            "Glenn Murray",
            MatchEventTeam(5, "Crystal Palace"),
            MatchEventTeam(65, "Swansea"),
            82,
            None
          ),
          Result
        )
      )
    }
  }
}
