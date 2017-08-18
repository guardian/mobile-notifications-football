package com.gu.mobile.notifications.football.models

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class MatchPhaseEventSpec extends Specification with Mockito {
  "MatchPhaseEvent" should {
    "Create a Kickoff event from timeline event 0:00" in {
      val event = mock[pa.MatchEvent]
      event.eventType returns "timeline"
      event.matchTime returns Some("0:00")
      MatchPhaseEvent.fromEvent(event) should beSome(KickOff)
    }
    "Return none event for timeline event 0:01" in {
      val event = mock[pa.MatchEvent]
      event.eventType returns "timeline"
      event.matchTime returns Some("0:01")
      MatchPhaseEvent.fromEvent(event) should beNone
    }
    "Create a fulltime event from full-time" in {
      val event = mock[pa.MatchEvent]
      event.eventType returns "full-time"
      MatchPhaseEvent.fromEvent(event) should beSome(FullTime)
    }
    "Create a halftime event from half-time" in {
      val event = mock[pa.MatchEvent]
      event.eventType returns "half-time"
      MatchPhaseEvent.fromEvent(event) should beSome(HalfTime)
    }
    "Create a secondhalf event from second-half" in {
      val event = mock[pa.MatchEvent]
      event.eventType returns "second-half"
      MatchPhaseEvent.fromEvent(event) should beSome(SecondHalf)
    }
  }
}
