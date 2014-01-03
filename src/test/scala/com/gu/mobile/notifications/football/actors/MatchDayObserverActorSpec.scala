package com.gu.mobile.notifications.football.actors

import akka.testkit.{ImplicitSender, TestKit, TestActorRef}
import akka.actor.ActorSystem
import org.scalatest.{Matchers, WordSpecLike, BeforeAndAfterAll}
import com.gu.mobile.notifications.football.actors.MatchDayObserverActor.{GoalEvent, CurrentLiveMatches, GetCurrentLiveMatches, UpdatedMatchDays}
import pa.{MatchDayTeam, MatchDay}
import org.joda.time.DateTime
import com.gu.mobile.notifications.football.lib.Pa.Goal

class MatchDayObserverActorSpec(_system: ActorSystem)
  extends TestKit(_system)
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  val matchDayObserver: TestActorRef[MatchDayObserverActor] = TestActorRef(MatchDayObserverActor.props())

  def this() = this(ActorSystem("MatchDayObserverActorSpec"))

  val beforeGoal = MatchDay(
    "id",
    new DateTime(),
    None,
    None,
    "leg",
    true,
    false,
    false,
    false,
    true,
    "LIVE",
    None,
    MatchDayTeam("1", "Manchester United", Some(0), None, None, None),
    MatchDayTeam("2", "Bolton Wanderers", Some(0), None, None, None),
    None,
    None,
    None
  )

  val afterGoal = beforeGoal.copy(
    homeTeam = MatchDayTeam("1", "Manchester United", Some(1), None, None, Some("Robert Berry (10)"))
  )

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A MatchDayObserverActor" when {
    "just initialized" should {
      "not have a match day set" in {
        matchDayObserver.underlyingActor.previousMatchDays shouldEqual None
      }

      "return an empty list when asked for current matches" in {
        matchDayObserver ! GetCurrentLiveMatches
        expectMsg(CurrentLiveMatches(Nil))
      }
    }

    "sent an updated match day with no goals" should {
      "update the match day" in {
        matchDayObserver ! UpdatedMatchDays(List(beforeGoal))
        matchDayObserver.underlyingActor.previousMatchDays shouldEqual Some(List(beforeGoal))
      }

      "return the match day when asked for it" in {
        matchDayObserver ! GetCurrentLiveMatches
        expectMsg(CurrentLiveMatches(List(beforeGoal)))
      }
    }

    "sent an updated match day with goals changed" should {
      "update the match day" in {
        matchDayObserver ! UpdatedMatchDays(List(afterGoal))
        matchDayObserver.underlyingActor.previousMatchDays shouldEqual Some(List(afterGoal))
      }

      "notify the sender about the new goal" in {
        expectMsg(GoalEvent(Goal(10, "Robert Berry", afterGoal.homeTeam), afterGoal))
      }
    }
  }
}
