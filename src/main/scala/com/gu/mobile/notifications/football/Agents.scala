package com.gu.mobile.notifications.football

import akka.agent.Agent
import com.gu.mobile.notifications.football.models.NotificationHistoryItem
import pa.MatchDay
import scala.concurrent.ExecutionContext.Implicits.global

object Agents {
  import SystemSetup._

  val notificationsHistory = Agent[List[NotificationHistoryItem]](Nil)
  val lastMatchDaysSeen = Agent[Option[List[MatchDay]]](None)
}
