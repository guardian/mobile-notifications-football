package com.gu.mobile.notifications.football.lib

import com.gu.mobile.notifications.client.ApiClient
import dispatch.Http
import scala.concurrent.{Future, ExecutionContext}
import com.gu.mobile.notifications.football.conf.GoalNotificationsConfig
import com.gu.mobile.notifications.client.models.{SendNotificationReply, Notification}
import com.gu.mobile.notifications.football.lib.Futures._
import com.gu.mobile.notifications.football.management.Metrics

object NotificationsClient extends ApiClient {
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def host: String = GoalNotificationsConfig.guardianNotificationsHost

  def httpClient: Http = Http

  /** Ugh, would be better to do something with the Http client itself directly */
  override def send(notification: Notification): Future[SendNotificationReply] = {
    val ftr = super.send(notification)
    ftr.recordTimeSpent(Metrics.notificationsResponseTime, Metrics.notificationsErrorResponseTime)
    ftr
  }
}
