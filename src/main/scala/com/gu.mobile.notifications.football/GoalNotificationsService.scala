package com.gu.mobile.notifications.football

import akka.actor.{ActorRef, Actor}
import spray.routing._
import spray.http._
import MediaTypes._
import akka.pattern.ask
import com.gu.mobile.notifications.football.actors.GoalNotificationsManagerActor
import com.gu.mobile.notifications.football.actors.MatchDayObserverActor.CurrentLiveMatches
import com.gu.mobile.notifications.football.lib.Pa._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.gu.mobile.notifications.football.actors.GoalNotificationSenderActor.SentNotification
import com.gu.mobile.notifications.client.models.{AndroidMessagePayload, IOSMessagePayload, Topic}

class GoalNotificationsServiceActor(val notificationsManager: ActorRef) extends Actor with GoalNotificationsService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}

/** Horrid HTML-puking functions */
trait Rendering {
  def renderIndex(matches: CurrentLiveMatches, history: List[SentNotification]) =
    <html>
      <head>
        <title>Goal Notifications Service</title>
      </head>

      <h1>Goal Notifications Service</h1>

      <h2>Current live matches</h2>
      {
        for (matchDay <- matches.matchDays) yield <p>{matchDay.summaryString}</p>
      }

      <h2>Goal notifications history</h2>
      {
        for (SentNotification(when, notification) <- history) yield
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

// this trait defines our service behavior independently from the service actor
trait GoalNotificationsService extends HttpService with Rendering {
  val notificationsManager: ActorRef

  implicit val timeout = Timeout(500 millis)

  val myRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            val liveMatchesFuture = (notificationsManager ? GoalNotificationsManagerActor.GetLiveMatches).mapTo[CurrentLiveMatches]
            val notificationHistoryFuture = (notificationsManager ? GoalNotificationsManagerActor.GetHistory).mapTo[List[SentNotification]]

            (liveMatchesFuture zip notificationHistoryFuture).map {
              case ((matches, history)) => renderIndex(matches, history)
            }
          }
        }
      }
    }
}