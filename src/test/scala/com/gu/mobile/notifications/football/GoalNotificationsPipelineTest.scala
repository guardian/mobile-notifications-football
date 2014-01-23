package com.gu.mobile.notifications.football

import org.scalatest.{ShouldMatchers, FreeSpec}
import rx.lang.scala.Observable
import pa.MatchDay
import scala.concurrent.duration._
import com.gu.mobile.notifications.client.models.Notification

class GoalNotificationsPipelineTest extends FreeSpec with ShouldMatchers with GoalNotificationsPipeline {

  val retrySendNotifications: Int = 1
  val UpdateInterval: FiniteDuration = 4.seconds
  val MaxHistoryLength: Int = 2

  var publishedMessages: List[String] = Nil

  override def getMatchDayStream(): Observable[List[MatchDay]] = MatchDaysTestData.matchDays

  override def publishNotifications: (Notification) => Unit = {
    notification: Notification => publishedMessages ::= notification.toString
  }

  "GoalNotificationPipeline" - {
    "When a goal occurs" - {
      "should publish a message to the SNS Queue" in {
        start()
        publishedMessages.head should (startWith("Notification(goal,mobile-notifications-football,"))
      }
    }
  }
}
