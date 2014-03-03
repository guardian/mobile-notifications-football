package com.gu.mobile.notifications.football.models

import pa.{MatchEvent => PaMatchEvent, Player}
import org.scalatest.{WordSpec, Matchers}

class MatchEventSpec extends WordSpec with Matchers {
  object PaMatchEvent {
    def empty = new PaMatchEvent(
      None,
      None,
      "",
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

  def timelineFixture(matchTime: String, minute: String) = PaMatchEvent.empty.copy(
    eventType = "timeline",
    matchTime = Some(matchTime),
    eventTime = Some(minute)
  )

  "MatchEvent.fromPa" should {
    "interpret the first timeline event as a Kick Off event" in {
      val kickOffEvent = timelineFixture("0:00", "0")
      MatchEvent.fromPa(kickOffEvent) should equal(Some(KickOff))
    }

    "ignore other timeline events" in {
      MatchEvent.fromPa(timelineFixture("0:01", "0")) should equal(None)
      MatchEvent.fromPa(timelineFixture("1:34", "1")) should equal(None)
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

      MatchEvent.fromPa(goalEvent) should equal(Some(Goal("Wes Hoolahan", 14, 3)))
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

      MatchEvent.fromPa(ownGoalEvent) should equal(Some(OwnGoal("Sebastien Bassong", 14, 41)))
    }
  }
}
