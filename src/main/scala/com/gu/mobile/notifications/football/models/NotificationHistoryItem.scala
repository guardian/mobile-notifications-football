package com.gu.mobile.notifications.football.models

import com.gu.mobile.notifications.client.models.GoalAlertPayload
import org.joda.time.DateTime

sealed trait NotificationHistoryItem {
  val payload: GoalAlertPayload
}

case class NotificationSent(sentAt: DateTime, payload: GoalAlertPayload) extends NotificationHistoryItem

case class NotificationFailed(gaveUpAt: DateTime, payload: GoalAlertPayload) extends NotificationHistoryItem