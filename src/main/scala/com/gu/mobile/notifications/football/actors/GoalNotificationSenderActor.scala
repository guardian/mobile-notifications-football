package com.gu.mobile.notifications.football.actors

import akka.actor._
import com.gu.mobile.notifications.client.ApiClient
import com.gu.mobile.notifications.football.lib.GoalNotificationBuilder
import scala.util.{Failure, Success}
import com.gu.mobile.notifications.client.models.Notification
import org.joda.time.DateTime

object GoalNotificationSenderActor {
  sealed trait Message
  case class SentNotification(when: DateTime, notification: Notification)

  def props(notificationsClient: ApiClient) = Props(classOf[GoalNotificationSenderActor], notificationsClient)
}

class GoalNotificationSenderActor(notificationsClient: ApiClient) extends Actor with ActorLogging {
  import context.dispatcher
  import GoalNotificationSenderActor._

  def receive = {
    case MatchDayObserverActor.GoalEvent(goal, matchDay) => {
      /** TODO Add circuit breaker logic here once Guardian Notifications has logic to ensure at most once message
        * delivery
        */
      val notification = GoalNotificationBuilder(goal, matchDay)

      notificationsClient.send(notification) onComplete {
        case Success(response) => {
          log.info(s"Successfully sent goal notification for ${goal.scorerName} at ${goal.minute} " +
            s"(match: ${matchDay.id}). Notification ID = ${response.messageId}")

          context.parent ! SentNotification(new DateTime(), notification)
        }

        case Failure(error) => {
          log.error(s"Unable to send goal notification for ${goal.scorerName} at ${goal.minute} " +
            s"(match: ${matchDay.id}): ${error.getMessage}")
        }
      }
    }
  }
}
