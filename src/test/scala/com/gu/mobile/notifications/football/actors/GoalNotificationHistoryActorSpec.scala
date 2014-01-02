package com.gu.mobile.notifications.football.actors

import akka.testkit.TestActorRef
import org.specs2.Specification
import com.gu.mobile.notifications.football.actors.GoalNotificationSenderActor.SentNotification
import org.joda.time.DateTime
import com.gu.mobile.notifications.client.models.{MessagePayloads, Target, Notification}
import akka.actor.ActorSystem

class GoalNotificationHistoryActorSpec extends Specification {
  implicit val testActorSystem = ActorSystem("test")

  val fixture1 = SentNotification(
    new DateTime(),
    Notification(
      `type` = "test",
      sender = "test@test.com",
      target = Target(Set.empty, Set.empty),
      timeToLiveInSeconds = 3600,
      payloads = MessagePayloads(None, None),
      metadata = Map.empty
    )
  )

  val fixture2 = SentNotification(
    new DateTime(),
    Notification(
      `type` = "test2",
      sender = "test@test.com",
      target = Target(Set.empty, Set.empty),
      timeToLiveInSeconds = 3600,
      payloads = MessagePayloads(None, None),
      metadata = Map.empty
    )
  )

  def is = "record history" ! {
    val actorRef = TestActorRef[GoalNotificationHistoryActor]

    actorRef ! fixture1

    actorRef.underlyingActor.history mustEqual List(fixture1)
  } ^ "limit the size of the history" ! {
    val actorRef = TestActorRef[GoalNotificationHistoryActor]

    (1 to (GoalNotificationHistoryActor.MaxSize * 3)) foreach { _ =>
      actorRef ! fixture1
    }

    actorRef ! fixture2

    val expectedHistory = fixture2 :: List.fill(GoalNotificationHistoryActor.MaxSize - 1)(fixture1)

    actorRef.underlyingActor.history mustEqual expectedHistory
  }
}
