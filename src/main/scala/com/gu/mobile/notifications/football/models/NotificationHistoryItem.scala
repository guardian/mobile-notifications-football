package com.gu.mobile.notifications.football.models

import com.gu.mobile.notifications.client.models.{Notification, SendNotificationReply}
import org.joda.time.DateTime

sealed trait NotificationHistoryItem {
  val notification: Notification
}

case class NotificationSent(
  sentAt: DateTime,
  notification: Notification,
  reply: SendNotificationReply
) extends NotificationHistoryItem

case class NotificationFailed(
  gaveUpAt: DateTime,
  notification: Notification
) extends NotificationHistoryItem