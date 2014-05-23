package com.gu.mobile.notifications.football.lib

import com.gu.mobile.notifications.client.ApiClient
import dispatch.Http
import scala.concurrent.{Future, ExecutionContext}
import com.gu.mobile.notifications.football.conf.GoalNotificationsConfig
import com.gu.mobile.notifications.client.models.{SendNotificationReply, Notification}
import com.gu.mobile.notifications.football.lib.Futures._
import com.gu.mobile.notifications.football.management.Metrics
import grizzled.slf4j.Logging

trait NotificationsClient extends ApiClient with SendsNotifications with Logging {
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def host: String = GoalNotificationsConfig.guardianNotificationsHost

  def httpClient: Http = Http

  override def send(notification: Notification): Future[SendNotificationReply] = {
    val ftr = super.send(notification)
    ftr.recordTimeSpent(Metrics.notificationsResponseTime, Metrics.notificationsErrorResponseTime)

    ftr onFailure {
      case error => logger.error("Error trying to send notification", error)
    }

    ftr
  }
}

/** To allow stubbing in tests */
trait SendsNotifications {
  def send(notification: Notification): Future[SendNotificationReply]
}