package com.gu.mobile.notifications.football

import akka.agent.Agent
import com.gu.mobile.notifications.football.models.NotificationHistoryItem
import scala.concurrent.ExecutionContext.Implicits.global

object Agents {
  import SystemSetup._

  val notificationsHistory = Agent[List[NotificationHistoryItem]](Nil)
}
