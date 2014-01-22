package com.gu.mobile.notifications.football

import org.scalatest.{ShouldMatchers, FreeSpec}
import rx.lang.scala.Observable
import scala.util.Try
import com.amazonaws.services.sns.model.PublishResult
import scala.util.Success
import pa.MatchDay
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

class GoalNotificationsPipelineTest extends FreeSpec with ShouldMatchers with GoalNotificationsPipeline {

  val retrySendNotifications: Int = 1
  val UpdateInterval: FiniteDuration = 4.seconds
  val buffer = ListBuffer[String]()
  val MaxHistoryLength: Int = 2

  var publishedMessages = ListBuffer[String]()

  override def getMatchDayStream(): Observable[List[MatchDay]] = MatchDaysTestData.matchDays

  override def publish(subject: String, message: String): Try[PublishResult] = {
    publishedMessages+=subject + " | " + message
    Success(new PublishResult())
  }


  "GoalNotificationPipeline" - {
    "When a goal occurs" - {
      "should send a message to the SNS Queue" in {
        start()
        publishedMessages.head should (include ("Goal notification created | Notification(goal,mobile-notifications-football,"))
      }
    }
  }
}
