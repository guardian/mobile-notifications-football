package com.gu.mobile.notifications.football.lib

import java.net.URI

import com.gu.football.models.{Goal, GoalContext, Score}
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.Importance.Major
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import pa.MatchDayTeam

class GoalNotificationBuilderSpec extends Specification {

  "A GoalNotificationBuilder" should {
    "Include basic metadata for all goal types" in new GoalContext {
      goalTypes.foreach { goalType =>
        val goal = baseGoal.copy(goalType = goalType)
        val notification = builder.build(goal, goalContext)
        notification.homeTeamScore mustEqual 2
        notification.awayTeamScore mustEqual 0
        notification.homeTeamName mustEqual "Liverpool"
        notification.awayTeamName mustEqual "Plymouth"
        notification.title mustEqual "The Guardian"
        notification.scorerName mustEqual "Steve"
        notification.goalMins mustEqual goal.minute
        notification.matchId mustEqual "match-1"
        notification.mapiUrl mustEqual new URI(s"http://localhost/sport/football/matches/match-1")
        notification.debug mustEqual false
        notification.addedTime must beNone
        notification.goalType mustEqual goalType
        notification.importance mustEqual Major
        notification.topic mustEqual Set(
          Topic(TopicTypes.FootballTeam, "1"),
          Topic(TopicTypes.FootballTeam, "2"),
          Topic(TopicTypes.FootballMatch, "match-1")
        )
        notification.scoringTeamName mustEqual "Liverpool"
        notification.otherTeamName mustEqual "Plymouth"
      }
    }

    "Included added time if goal is scored in added time" in new GoalContext {
      goalTypes.foreach { goalType =>
        val notification = builder.build(baseGoal.copy(goalType = goalType, addedTime = Some("4:32")), goalContext)
        notification.addedTime must beSome("4:32")
      }
    }

    "build a notification for a regular goal" in new GoalContext {
      val notification = builder.build(baseGoal.copy(goalType = DefaultGoalType), goalContext)
      notification.message mustEqual s"""Liverpool 2-0 Plymouth
                                        |Steve 5min""".stripMargin
    }

    "build a notification for an own goal" in new GoalContext {
      val notification = builder.build(baseGoal.copy(goalType = OwnGoalType), goalContext)
      notification.message mustEqual s"""Liverpool 2-0 Plymouth
                                        |Steve 5min (o.g.)""".stripMargin
    }

    "build a notification for a penalty goal"  in new GoalContext {
      val notification = builder.build(baseGoal.copy(goalType = PenaltyGoalType), goalContext)
      notification.message mustEqual s"""Liverpool 2-0 Plymouth
                                        |Steve 5min (pen)""".stripMargin

    }

    "build a notification for a regular goal in extra time" in new GoalContext {
      val notification = builder.build(baseGoal.copy(goalType = DefaultGoalType, addedTime = Some("4:32")), goalContext)
      notification.message mustEqual s"""Liverpool 2-0 Plymouth
                                        |Steve 5min (+4:32)""".stripMargin

    }

    "build a notification for an own goal in extra time" in new GoalContext {
      val notification = builder.build(baseGoal.copy(goalType = OwnGoalType, addedTime = Some("4:32")), goalContext)
      notification.message mustEqual s"""Liverpool 2-0 Plymouth
                                        |Steve 5min (o.g. +4:32)""".stripMargin
    }

    "build a notification for a penalty goal in extra time" in new GoalContext {
      val notification = builder.build(baseGoal.copy(goalType = PenaltyGoalType, addedTime = Some("4:32")), goalContext)
      notification.message mustEqual s"""Liverpool 2-0 Plymouth
                                        |Steve 5min (pen +4:32)""".stripMargin
    }
  }

  trait GoalContext extends Scope {
    val goalTypes = List(OwnGoalType, PenaltyGoalType, DefaultGoalType)
    val builder = new GoalNotificationBuilder("http://localhost")
    val home = MatchDayTeam("1", "Liverpool", None, None, None, None)
    val away = MatchDayTeam("2", "Plymouth", None, None, None, None)
    val baseGoal = Goal(DefaultGoalType, "Steve", home, away, 5, None)
    val goalContext = GoalContext(home, away, "match-1", Score(2, 0))
  }
}
