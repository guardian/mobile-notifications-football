package com.gu.mobile.notifications.football

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import com.gu.mobile.notifications.football.lib.Pa._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.gu.mobile.notifications.football.models._
import com.gu.mobile.notifications.football.lib.{GoalNotificationBuilder, PaFootballClient, ExpiringTopics, PaExpiringTopics}
import ExpirationJsonImplicits._
import pa.MatchDay
import scala.Some
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.football.models.NotificationSent

class GoalNotificationsServiceActor extends Actor with GoalNotificationsService {
  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)

  override val expiringTopics: ExpiringTopics = PaExpiringTopics(PaFootballClient)
}

/** Horrid HTML-puking functions */
trait Rendering {
  def renderIndex(matches: List[MatchDay], history: List[NotificationHistoryItem]) =
    <html>
      <head>
        <title>Goal Notifications Service</title>
      </head>

      <h1>Goal Notifications Service</h1>

      <h2>Current live matches</h2>
      {
        for (matchDay <- matches) yield <p>{matchDay.summaryString}</p>
      }

      <h2>Goal notifications history</h2>
      {
        for (NotificationSent(when, notification, _) <- history collect { case n: NotificationSent => n }) yield
          <dl>
            <dt>Time sent</dt>
            <dd>{ when.toString() }</dd>

            <dt>TTL (secs)</dt>
            <dd>{ notification.timeToLiveInSeconds }</dd>

            <dt>Topics</dt>
            <dd>{ renderTopics(notification.target.topics.toSeq) }</dd>

            <dt>Android Payload</dt>
            <dd>{ renderAndroidPayload(notification.payloads.android.get) }</dd>

            <dt>iOS Payload</dt>
            <dd>{ renderIosPayload(notification.payloads.ios.get) }</dd>
          </dl>
      }
    </html>

  def renderTopics(topics: Seq[Topic]) = <ul>{
     topics map { topic: Topic => <li>{ topic.`type` }: { topic.name }</li> }
    }</ul>

  def renderIosPayload(payload: IOSMessagePayload) = <table>
      <tr>
        <th>Body</th>
        <td>{ payload.body }</td>
      </tr>
      {
        for {
          (k, v) <- payload.customProperties
        } yield <tr><th>{ k }</th><td>{ v }</td></tr>
      }
    </table>

  def renderAndroidPayload(payload: AndroidMessagePayload) = <table>
    {
      for {
        (k, v) <- payload.body
      } yield <tr><th>{ k }</th><td>{ v }</td></tr>
    }
  </table>
}

case class GoalAndMetadata(goal: Goal, metadata: EventFeedMetadata)

// this trait defines our service behavior independently from the service actor
trait GoalNotificationsService extends HttpService with Rendering {
  implicit val timeout = Timeout(500 millis)

  val expiringTopics: ExpiringTopics

  val myRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            Agents.lastMatchDaysSeen.get() match {
              case Some(matchDays) => renderIndex(matchDays, Agents.notificationsHistory.get())
              case None => <p>Not contacted PA yet</p>
            }
          }
        }
      }
    } ~
    path("expired-topics") {
      post {
        decompressRequest() {
          entity(as[ExpirationRequest]) { request =>
            detach() {
              complete {
                expiringTopics.getExpired(request.topics) map { expiredTopics =>
                  ExpirationResponse(expiredTopics)
                }
              }
            }
          }
        }
      }
    } ~
    path("send-test-notification") {
      post {
        decompressRequest() {
          entity(as[GoalAndMetadata]) { case GoalAndMetadata(goal, metadata) =>
            detach() {
              respondWithMediaType(`text/html`) {
                complete {
                  GoalNotificationsPipeline.send(GoalNotificationBuilder(goal, metadata)) map { _ =>
                    <p>Sent a test notification</p>
                  }
                }
              }
            }
          }
        }
      }
    }
}