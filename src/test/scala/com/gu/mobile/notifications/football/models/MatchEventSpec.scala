package com.gu.mobile.notifications.football.models

import pa.{Team, MatchEvents, Player}
import pa.Parser.parseMatchEvents
import org.scalatest.{WordSpec, Matchers}
import com.gu.mobile.notifications.football.lib.ResourcesHelper
import com.gu.mobile.notifications.football.helpers.EmptyInstances

class MatchEventSpec extends WordSpec with Matchers with ResourcesHelper with EmptyInstances {
  def timelineFixture(matchTime: String, minute: String) = PaMatchEvent.empty.copy(
    eventType = "timeline",
    matchTime = Some(matchTime),
    eventTime = Some(minute)
  )

  val homeTeamFixture = Team("2", "Aston Villa")
  val awayTeamFixture = Team("14", "Norwich")

  val matchEventsFixture = MatchEvents(
    homeTeamFixture,
    awayTeamFixture,
    Nil,
    isResult = false
  )

  val homeTeam = MatchEventTeam(2, "Aston Villa")
  val awayTeam = MatchEventTeam(14, "Norwich")

  "MatchEvent.fromMatchEvent" should {
    "interpret the first timeline event as a Kick Off event" in {
      val kickOffEvent = timelineFixture("0:00", "0")
      MatchEvent.fromMatchEvent(matchEventsFixture)(kickOffEvent) should equal(Some(KickOff))
    }

    "ignore other timeline events" in {
      MatchEvent.fromMatchEvent(matchEventsFixture)(timelineFixture("0:01", "0")) should equal(None)
      MatchEvent.fromMatchEvent(matchEventsFixture)(timelineFixture("1:34", "1")) should equal(None)
    }

    "interpret a goal event as a goal event" in {
      val goalEvent = PaMatchEvent.empty.copy(
        teamID = Some("14"),
        id = Some("18877842"),
        eventType = "goal",
        matchTime = Some("(3)"),
        eventTime = Some("3"),
        players = List(
          Player("237552", "14", "Wes Hoolahan")
        ),
        how = Some("Left Foot"),
        whereFrom = Some("Centre Penalty Area"),
        whereTo = Some("Left Low")
      )

      MatchEvent.fromMatchEvent(matchEventsFixture)(goalEvent) should equal(Some(
        Goal("Wes Hoolahan", awayTeam, homeTeam, 3, None))
      )
    }

    "interpret an own goal event as an own goal event" in {
      val ownGoalEvent = PaMatchEvent.empty.copy(
        teamID = Some("14"),
        id = Some("18878420"),
        eventType = "own goal",
        matchTime = Some("(41)"),
        eventTime = Some("41"),
        players = List(
          Player("322560", "14", "Sebastien Bassong")
        ),
        how = Some("Right Foot"),
        whereTo = Some("Right Low")
      )

      MatchEvent.fromMatchEvent(matchEventsFixture)(ownGoalEvent) should equal(Some(
        OwnGoal("Sebastien Bassong", homeTeam, awayTeam, 41, None))
      )
    }

    "interpret a penalty goal as a penalty goal event" in {
      val penaltyGoalEvent = PaMatchEvent.empty.copy(
        teamID = Some("14"),
        id = Some("18879192"),
        eventType = "goal from penalty",
        matchTime = Some("(82)"),
        eventTime = Some("82"),
        players = List(
          Player("285753", "14", "Glenn Murray")
        ),
        how = Some("Right Foot"),
        whereFrom = Some("Penalty Spot"),
        whereTo = Some("Right High")
      )

      MatchEvent.fromMatchEvent(matchEventsFixture)(penaltyGoalEvent) should equal(Some(
        PenaltyGoal("Glenn Murray", awayTeam, homeTeam, 82, None)
      ))
    }
  }

  "MatchEvent.fromMatchEvents" should {
    "correctly report the events from a match events feed" in {
      val Some(matchEvents) = parseMatchEvents(slurpOrDie("pa/football/match/events/key/3704151.xml"))

      MatchEvent.fromMatchEvents(matchEvents) should equal(List(
        KickOff,
        Goal("Wes Hoolahan", awayTeam, homeTeam, 3, None),
        Goal("Christian Benteke", homeTeam, awayTeam, 25, None),
        Goal("Christian Benteke", homeTeam, awayTeam, 27, None),
        Goal("Leandro Bacuna", homeTeam, awayTeam, 37, None),
        OwnGoal("Sebastien Bassong", homeTeam, awayTeam, 41, None),
        Result
      ))
    }

    "correctly report events in extra time from a match events feed" in {
      val Some(matchEvents) = parseMatchEvents(slurpOrDie("pa/football/match/events/key/3704203.xml"))

      val stevenage = MatchEventTeam(1073, "Stevenage")
      val crawleyTown = MatchEventTeam(188, "Crawley Town")

      MatchEvent.fromMatchEvents(matchEvents) should equal(List(
        KickOff,
        Goal("Bira Dembele", stevenage, crawleyTown, 21, None),
        Goal("Matt Tubbs", crawleyTown, stevenage, 90, Some("1:15")),
        Result
      ))
    }
  }
}
