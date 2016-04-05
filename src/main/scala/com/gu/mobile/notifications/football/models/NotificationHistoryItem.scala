package com.gu.mobile.notifications.football.models

import com.gu.mobile.notifications.client.models.GoalAlertPayload
import org.joda.time.DateTime

sealed trait NotificationHistoryItem {
  val notification: GoalAlertPayload
}

case class NotificationSent(
  sentAt: DateTime,
  notification: GoalAlertPayload
) extends NotificationHistoryItem

case class NotificationFailed(
  gaveUpAt: DateTime,
  notification: GoalAlertPayload
) extends NotificationHistoryItem