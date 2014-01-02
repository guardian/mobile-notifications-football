package com.gu.mobile.notifications.football.actors

import akka.testkit.TestActorRef
import com.gu.mobile.notifications.football.actors.GoalNotificationSenderActor.SentNotification
import org.joda.time.DateTime
import com.gu.mobile.notifications.client.models.{MessagePayloads, Target, Notification}
import TestActorSystem._
import org.scalatest.WordSpec

class GoalNotificationHistoryActorSpec extends WordSpec {
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

  "GoalNotificationHistoryActor" when {
    "empty and sent a new notification" should {
      "prepend it to its history" in {
        val actorRef = TestActorRef[GoalNotificationHistoryActor]

        actorRef ! fixture1

        actorRef.underlyingActor.history == List(fixture1)
      }
    }

    "full and sent a new notification" should {
      val actorRef = TestActorRef[GoalNotificationHistoryActor]
      (1 to (GoalNotificationHistoryActor.MaxSize * 3)) foreach { _ =>
        actorRef ! fixture1
      }
      actorRef ! fixture2

      "prepend it to its history" in {
        actorRef.underlyingActor.history.headOption == Some(fixture2)
      }

      "limit the size of its history to MaxSize" in {
        actorRef.underlyingActor.history.size == GoalNotificationHistoryActor.MaxSize
      }
    }
  }
}
