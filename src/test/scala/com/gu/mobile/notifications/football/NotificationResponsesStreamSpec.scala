package com.gu.mobile.notifications.football

import com.gu.mobile.notifications.client.HttpProvider
import org.scalatest.{Matchers, WordSpec}
import com.gu.mobile.notifications.client.models.legacy.{MessagePayloads, Target, Notification}
import scala.concurrent.Future
import rx.lang.scala.Observable
import com.gu.mobile.notifications.football.models.{NotificationFailed, NotificationSent}
import scala.concurrent.ExecutionContext.Implicits.global
import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews

class NotificationResponsesStreamSpec extends WordSpec with Matchers {

  val notificationFixture = Notification(
    BreakingNews,
    "test",
    "test@theguardian.com",
    Target(Set.empty, Set.empty),
    3600,
    MessagePayloads(None, None),
    Map.empty)

  val notificationFixture2 = Notification(
    BreakingNews,
    "test2",
    "test@gu.com",
    Target(Set.empty, Set.empty),
    3600,
    MessagePayloads(None, None),
    Map.empty
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
        val stream = new NotificationResponseStream with StubSuccessClient {
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
        val stream = new NotificationResponseStream with StubFailureClient {
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
        val stream = new NotificationResponseStream with StubFailOnceClient {
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

  trait StubSuccessClient extends HttpProvider {
    self: NotificationResponseStream =>
    override val host = ""
    override def get(url: String): Future[HttpResponse] = Future.successful(HttpOk(200, "OK"))
    override def post(urlString: String, contentType: ContentType, body: Array[Byte]): Future[HttpResponse] =
      Future.successful(HttpOk(200, """{"messageId":"test"}"""))
  }

  trait StubFailureClient extends HttpProvider {
    self: NotificationResponseStream =>
    override val host = ""
    override def get(url: String): Future[HttpResponse] = Future.successful(HttpError(500, "Boom"))
    override def post(urlString: String, contentType: ContentType, body: Array[Byte]): Future[HttpResponse] =
      Future.successful(HttpError(500, "Kaboom"))
  }

  trait StubFailOnceClient extends HttpProvider {
    self: NotificationResponseStream =>
    override val host = ""
    var hasFailed = false
    override def post(urlString: String, contentType: ContentType, body: Array[Byte]): Future[HttpResponse] = if (hasFailed) {
      Future.successful(HttpOk(200, """{"messageId":"test"}"""))
    } else {
      hasFailed = true
      Future.successful(HttpError(500, "Kaboom"))
    }
    override def get(url: String): Future[HttpResponse] = Future.successful(HttpError(500, "Boom"))
  }
}
