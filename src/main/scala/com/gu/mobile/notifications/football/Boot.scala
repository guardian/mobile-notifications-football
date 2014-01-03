package com.gu.mobile.notifications.football

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import com.gu.mobile.notifications.football.management.MobileNotificationsManagementServer
import com.gu.mobile.notifications.football.lib._
import com.gu.mobile.notifications.football.lib.PaMatchDayClient
import rx.lang.scala.Observable
import lib.Observables._
import scala.concurrent.ExecutionContext.Implicits.global
import com.gu.mobile.notifications.football.models.{NotificationHistoryItem, NotificationFailed, NotificationSent}
import org.joda.time.DateTime

object Boot extends App {
  val RetrySendNotifications = 5

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("goal-notifications-system")

  val goalEventStream = Pa.goalNotificationStream(PaMatchDayClient(PaFootballClient).observable)
  val notificationStream = goalEventStream map {
    case GoalEvent(goal, matchDay) => GoalNotificationBuilder(goal, matchDay)
  }

  val notificationSendHistory: Observable[NotificationHistoryItem] = notificationStream flatMap { notification =>
    NotificationsClient.send(notification).asObservable.retry(RetrySendNotifications) map { reply =>
      NotificationSent(
        new DateTime(),
        notification,
        reply
      )
    } onErrorResumeNext {
      Observable(NotificationFailed(
        new DateTime(),
        notification
      ))
    }
  }

  // create and start our service actor
  val service = system.actorOf(
    Props(classOf[GoalNotificationsServiceActor]),
    "goal-notifications-http-service"
  )

  implicit val timeout = Timeout(5.seconds)
  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)

  MobileNotificationsManagementServer.start()
}
