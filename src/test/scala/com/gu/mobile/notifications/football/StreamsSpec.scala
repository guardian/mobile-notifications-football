package com.gu.mobile.notifications.football

import org.scalatest.{Matchers, WordSpec}
import com.gu.mobile.notifications.football.lib.SendsNotifications
import com.gu.mobile.notifications.client.models.{MessagePayloads, Target, SendNotificationReply, Notification}
import scala.concurrent.Future
import rx.lang.scala.Observable
import com.gu.mobile.notifications.football.models.{NotificationHistoryItem, NotificationFailed, NotificationSent}
import scala.concurrent.ExecutionContext.Implicits.global

class StreamsSpec extends WordSpec with Matchers {
  val stubSuccessClient = new SendsNotifications {
    def send(notification: Notification): Future[SendNotificationReply] =
      Future.successful(SendNotificationReply("test"))
  }

  val stubFailureClient = new SendsNotifications {
    def send(notification: Notification): Future[SendNotificationReply] = Future.failed(new RuntimeException)
  }

  /** Simulates a client that intermittently fails, so that we can check whether temporary errors are swallowed below */
  val stubFailOnceClient = new SendsNotifications {
    var hasFailed = false

    def send(notification: Notification): Future[SendNotificationReply] = if (hasFailed) {
        stubSuccessClient.send(notification)
      } else {
        hasFailed = true
        stubFailureClient.send(notification)
      }
  }

  val notificationFixture = Notification(
    "test",
    "test@theguardian.com",
    Target(Set.empty, Set.empty),
    3600,
    MessagePayloads(None, None),
    Map.empty)

  val notificationFixture2 = Notification(
    "test2",
    "test@gu.com",
    Target(Set.empty, Set.empty),
    3600,
    MessagePayloads(None, None),
    Map.empty
  )

  val notificationFixtures = List(notificationFixture, notificationFixture2)

  val notificationFixturesObservable = Observable(notificationFixtures: _*)

  implicit class RichSeq[A](xs: Seq[A]) {
    /** Filters the list for items of the given subtype of A */
    def withType[B <: A](implicit m: Manifest[B]) = xs collect { case b: B => b }
  }

  "Streams.notificationResponses" when {
    "run over an API client that always succeeds" should {
      "produce a stream of success responses" in {
        val responses = Streams.notificationResponses(notificationFixturesObservable, stubSuccessClient, 0)
          .toBlockingObservable.toList

        responses should have length 2

        val successes = responses.withType[NotificationSent]
        successes should have length 2
        successes.map(_.notification).toSet shouldEqual notificationFixtures.toSet
      }
    }

    "run over an API client that always fails" should {
      "produce a stream of failure responses" in {
        val responses = Streams.notificationResponses(notificationFixturesObservable, stubFailureClient, 5)
          .toBlockingObservable.toList

        responses should have length 2
        val failures = responses.withType[NotificationFailed]
        failures should have length 2
        failures.map(_.notification).toSet shouldEqual notificationFixtures.toSet
      }
    }

    "run over an API client that fails once with more than 1 retries set" should {
      "produce a stream of success responses" in {
        val responses = Streams.notificationResponses(notificationFixturesObservable, stubFailOnceClient, 2)
          .toBlockingObservable.toList
        responses should have length 2
        val successes = responses.withType[NotificationSent]

        successes should have length 2
        successes.map(_.notification).toSet shouldEqual notificationFixtures.toSet
      }
    }
  }
}
