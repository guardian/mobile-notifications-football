package com.gu.mobile.notifications.football

import java.net.URI
import java.util.UUID

import com.gu.mobile.notifications.client._
import com.gu.mobile.notifications.client.models.Importance.{Importance, Major}
import com.gu.mobile.notifications.client.models._
import org.scalatest.{Matchers, WordSpec}
import com.gu.mobile.notifications.client.models.legacy.{MessagePayloads, Notification, Target}
import com.gu.mobile.notifications.football.lib.NotificationsClient

import scala.concurrent.{ExecutionContext, Future}
import rx.lang.scala.Observable
import com.gu.mobile.notifications.football.models.{NotificationFailed, NotificationSent}

import scala.concurrent.ExecutionContext.Implicits.global


class NotificationResponsesStreamSpec extends WordSpec with Matchers {

  val notificationFixture = GoalAlertPayload(
    id = "test",
    title = "title",
    message = "message",
    sender = "test@theguardian.com",
    goalType = OwnGoalType,
    awayTeamName = "awayTeam",
    awayTeamScore = 1,
    homeTeamName = "homeTeam",
    homeTeamScore = 2,
    scoringTeamName = "homeTeam",
    scorerName = "scorer",
    goalMins = 42,
    otherTeamName = "awayTeam",
    matchId = "matchId",
    mapiUrl = new URI("url"),
    importance = Major,
    topic = Set.empty,
    debug = false,
    addedTime = None
  )

  val notificationFixture2 = GoalAlertPayload(
    id = "test2",
    title = "title1",
    message = "message2",
    sender = "test@gu.com",
    goalType = OwnGoalType,
    awayTeamName = "awayTeam2",
    awayTeamScore = 2,
    homeTeamName = "homeTeam2",
    homeTeamScore = 3,
    scoringTeamName = "homeTeam2",
    scorerName = "scorer2",
    goalMins = 35,
    otherTeamName = "awayTeam2",
    matchId = "matchId2",
    mapiUrl = new URI("url"),
    importance = Major,
    topic = Set.empty,
    debug = false,
    addedTime = None
  )


  val notificationFixtures = List(notificationFixture, notificationFixture2)

  val notificationFixturesObservable = Observable.items(notificationFixtures: _*)

  implicit class RichSeq[A](xs: Seq[A]) {
    /** Filters the list for items of the given subtype of A */
    def withType[B <: A](implicit m: Manifest[B]) = xs collect { case b: B => b }
  }

  "Streams.notificationResponses" when {
    "run over an API client that always succeeds" should {
      "produce a stream of success responses" in {
        val stream = new NotificationResponseStream with NotificationsClient {
          override val apiClient = SuccessApiClient
          val retrySendNotifications: Int = 0
        }

        val responses = stream.getNotificationResponses(notificationFixturesObservable)
          .toBlockingObservable.toList

        responses should have length 2

        val successes = responses.withType[NotificationSent]
        successes should have length 2
        successes.map(_.notification).toSet shouldEqual notificationFixtures.toSet
      }
    }

    "run over an API client that always fails" should {
      "produce a stream of failure responses" in {
        val stream = new NotificationResponseStream with NotificationsClient {
          override val apiClient = FailureApiClient
          val retrySendNotifications: Int = 5
        }
        val responses = stream.getNotificationResponses(notificationFixturesObservable)
          .toBlockingObservable.toList

        responses should have length 2
        val failures = responses.withType[NotificationFailed]
        failures should have length 2
        failures.map(_.notification).toSet shouldEqual notificationFixtures.toSet
      }
    }

    "run over an API client that fails once with more than 1 retries set" should {
      "produce a stream of success responses" in {
        val stream = new NotificationResponseStream with NotificationsClient {
          override val apiClient = FailOnceClient
          val retrySendNotifications: Int = 2
        }

        val responses = stream.getNotificationResponses(notificationFixturesObservable)
          .toBlockingObservable.toList
        responses should have length 2
        val successes = responses.withType[NotificationSent]

        successes should have length 2
        successes.map(_.notification).toSet shouldEqual notificationFixtures.toSet
      }
    }
  }

  object FailureApiClient extends ApiClient {
    override def clientId: String = "failureClient"
    override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext) = Future.successful(Left(ApiHttpError(500)))
  }
  object SuccessApiClient extends ApiClient {
    override def clientId: String = "successClient"
    override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext) = Future.successful(Right())
  }

  object FailOnceClient extends ApiClient {
    var hasFailed = false

    override def clientId: String = "failOnce"

    override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext) =
      if (hasFailed) Future.successful(Right())
      else {
        hasFailed = true
        Future.successful(Left(ApiHttpError(500)))
      }

  }
}
