package com.gu.mobile.notifications.football.actors

import akka.testkit.TestActorRef
import TestActorSystem._
import pa.MatchDay
import scala.concurrent.Future
import com.gu.mobile.notifications.football.lib.MatchDayClient
import com.gu.mobile.notifications.football.actors.MatchDayObserverActor.Refresh

/*
class MatchDayObserverActorSpec extends Specification {
  def matchDayClientFixture(matchDay: Future[List[MatchDay]]) = new MatchDayClient {
    /** Today's match day */
    def today: Future[List[MatchDay]] = matchDay
  }

  val successfulEmptyTestActor: TestActorRef[MatchDayObserverActor] =
    TestActorRef(MatchDayObserverActor.props(matchDayClientFixture(Future.successful(Nil))))

  def is = "initially contain no match days" ! {
    successfulEmptyTestActor.underlyingActor.previousMatchDays mustEqual None
  } ^ "return an empty list when asked for match days but has none" ! {

  } ^ "update match days after being told to refresh" ! {
    successfulEmptyTestActor ! Refresh

  }
}
*/