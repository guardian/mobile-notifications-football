package com.gu.mobile.notifications.football

import com.gu.mobile.notifications.client.ApiClient
import com.gu.mobile.notifications.client.models.OwnGoalType
import com.gu.mobile.notifications.football.notificationbuilders.{GoalNotificationBuilder, MatchStatusNotificationBuilder}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import pa.{MatchDay, MatchDayTeam, MatchEvent, Player, Round, Stage}
import com.gu.mobile.notifications.client.models.Importance.Major
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.football.models.{FootballMatchEvent, Goal, GoalContext, Score}
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class EventConsumerImplSpec(implicit ev: ExecutionEnv) extends Specification with Mockito {
  "An EventComsumer" should {
    "Send a goal alert and status event for goals" in new MatchEventsContext {
      val goalNotificationBuilder = mock[GoalNotificationBuilder]
      val matchStatusNotificationBuilder = mock[MatchStatusNotificationBuilder]
      val notificationClient = mock[ApiClient]
      val consumer = new EventConsumerImpl(
        goalNotificationBuilder,
        matchStatusNotificationBuilder,
        notificationClient
      )
      notificationClient.send(any[NotificationPayload])(any[ExecutionContext]) returns Future.successful(Right(()))

      consumer.receiveEvent(matchInfo, List.empty, matchEvent) should beEqualTo(()).await

      there was one(goalNotificationBuilder).build(any[Goal], any[MatchDay], any[List[FootballMatchEvent]])
      there was one(matchStatusNotificationBuilder).build(any[FootballMatchEvent], any[MatchDay], any[List[FootballMatchEvent]])
      there were two(notificationClient).send(any[NotificationPayload])(any[ExecutionContext])
    }
  }

  trait MatchEventsContext extends Scope {
    val goalTypes = List(OwnGoalType, PenaltyGoalType, DefaultGoalType)
    val builder = new MatchStatusNotificationBuilder("http://localhost")
    val home = MatchDayTeam("1", "Liverpool", None, None, None, None)
    val away = MatchDayTeam("2", "Plymouth", None, None, None, None)
    val matchEvent = MatchEvent(
      id = Some("456"),
      teamID = Some("123"),
      eventType = "goal",
      matchTime = Some("5"),
      eventTime = Some("5"),
      addedTime = None,
      players = List(Player("1", "123", "Player1"), Player("2", "123", "Player2")),
      reason = None,
      how = None,
      whereFrom = None,
      whereTo = None,
      distance = None,
      outcome = None
    )
    val baseGoal = Goal(DefaultGoalType, "Steve", home, away, 5, None)
    val goalContext = GoalContext(home, away, "match-1", Score(2, 0))
    val matchInfo = MatchDay(
      id = "some-match-id",
      date = DateTime.parse("2000-01-01T00:00:00Z"),
      competition = None,
      stage = Stage("1"),
      round = Round("1", None),
      leg = "home",
      liveMatch = true,
      result =  false,
      previewAvailable = false,
      reportAvailable = false,
      lineupsAvailable = false,
      matchStatus = "HT",
      attendance = None,
      homeTeam = home,
      awayTeam = away,
      referee = None,
      venue = None,
      comments = None
    )
  }
}
