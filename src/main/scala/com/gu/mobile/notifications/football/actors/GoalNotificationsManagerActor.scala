package com.gu.mobile.notifications.football.actors

import akka.actor.{Props, OneForOneStrategy, Actor}
import akka.actor.SupervisorStrategy.Restart
import com.gu.mobile.notifications.client.ApiClient
import akka.util.Timeout
import akka.pattern.{ask, pipe}
import scala.concurrent.duration._
import com.gu.mobile.notifications.football.lib.MatchDayClient

object GoalNotificationsManagerActor {
  sealed trait Message

  case object GetHistory extends Message
  case object GetLiveMatches extends Message

  def props(matchDayClient: MatchDayClient, notificationsApi: ApiClient) =
    Props(classOf[GoalNotificationsManagerActor], matchDayClient, notificationsApi)
}

/** What a mouthful! It's like being in Java-land!
  *
  * This class just basically manages death watch on the other classes and allows the HTTP API to query the internal
  * state of the system
  */
class GoalNotificationsManagerActor(matchDayClient: MatchDayClient, notificationsApi: ApiClient) extends Actor {
  import GoalNotificationsManagerActor._
  import GoalNotificationSenderActor._
  import MatchDayObserverActor._
  import GoalNotificationHistoryActor._

  import context.dispatcher

  val matchDayObserver = context.system.actorOf(MatchDayObserverActor.props(matchDayClient))
  val goalNotificationSender = context.system.actorOf(GoalNotificationSenderActor.props(notificationsApi))
  val notificationsHistory = context.system.actorOf(GoalNotificationHistoryActor.props())

  override val supervisorStrategy = OneForOneStrategy() { case _ => Restart }

  implicit val getTimeout = Timeout(500 millis)

  def receive = {
    case goalEvent: GoalEvent => goalNotificationSender ! goalEvent
    case notificationSent: SentNotification => notificationsHistory ! notificationSent

    case GetHistory => (notificationsHistory ? Get) pipeTo sender
    case GetLiveMatches => (matchDayObserver ? GetCurrentLiveMatches) pipeTo sender
  }
}
