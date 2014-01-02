package com.gu.mobile.notifications.football

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import com.gu.mobile.notifications.football.management.MobileNotificationsManagementServer
import com.gu.mobile.notifications.football.actors.{GoalNotificationsManagerActor, GoalNotificationSenderActor, MatchDayObserverActor}
import com.gu.mobile.notifications.football.lib.{PaMatchDayClient, NotificationsClient, PaFootballClient}

object Boot extends App {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("goal-notifications-system")

  val notificationsManager = system.actorOf(
    GoalNotificationsManagerActor.props(PaMatchDayClient(PaFootballClient), NotificationsClient),
    "goal-notifications-manager"
  )
  // create and start our service actor
  val service = system.actorOf(
    Props(classOf[GoalNotificationsServiceActor], notificationsManager),
    "goal-notifications-http-service"
  )

  implicit val timeout = Timeout(5.seconds)
  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)

  MobileNotificationsManagementServer.start()
}
