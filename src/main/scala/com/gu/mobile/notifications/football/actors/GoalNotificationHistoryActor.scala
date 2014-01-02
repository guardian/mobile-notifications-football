package com.gu.mobile.notifications.football.actors

import akka.actor.{Props, Actor}
import com.gu.mobile.notifications.football.actors.GoalNotificationSenderActor.SentNotification

object GoalNotificationHistoryActor {
  val MaxSize = 20

  sealed trait Message

  case object Get extends Message

  def props() = Props(classOf[GoalNotificationHistoryActor])
}

/** Maintains a history of goal notifications we've sent */
class GoalNotificationHistoryActor extends Actor {
  import GoalNotificationHistoryActor._

  var history: List[SentNotification] = Nil

  def receive = {
    case sent: SentNotification => history = (sent :: history) take MaxSize

    case Get => sender ! history
  }
}
