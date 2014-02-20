package com.gu.mobile.notifications.football.lib

import com.gu.mobile.notifications.client.ApiClient
import dispatch.Http
import scala.concurrent.{Future, ExecutionContext}
import com.gu.mobile.notifications.football.conf.{MobileNotificationsFootballSwitches, GoalNotificationsConfig}
import com.gu.mobile.notifications.client.models.{SendNotificationReply, Notification}
import com.gu.mobile.notifications.football.lib.Futures._
import com.gu.mobile.notifications.football.management.Metrics
import grizzled.slf4j.Logging

trait NotificationsClient extends ApiClient with SendsNotifications with Logging {
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def host: String = GoalNotificationsConfig.guardianNotificationsHost

  def httpClient: Http = Http

  /** Ugh, would be better to do something with the Http client itself directly */
  override def send(notification: Notification): Future[SendNotificationReply] = {
    if (MobileNotificationsFootballSwitches.sendNotifications.enabled) {
      val ftr = super.send(notification)
      ftr.recordTimeSpent(Metrics.notificationsResponseTime, Metrics.notificationsErrorResponseTime)

      ftr onFailure {
        case error => logger.error("Error trying to send notification", error)
      }

      ftr
    } else {
      Future.failed(new RuntimeException("Sending notifications is disabled."))
    }
  }
}

/** To allow stubbing in tests */
trait SendsNotifications {
  def send(notification: Notification): Future[SendNotificationReply]
}