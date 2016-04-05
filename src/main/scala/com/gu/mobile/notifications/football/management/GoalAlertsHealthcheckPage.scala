package com.gu.mobile.notifications.football.management

import com.gu.management._
import com.gu.mobile.notifications.football.GoalNotificationsPipeline
import scala.concurrent.duration._
import scala.concurrent.Await
import com.gu.mobile.notifications.client.models.{Unhealthy, Ok}
import com.gu.management.{HttpRequest, PlainTextResponse}
import scala.concurrent.ExecutionContext.Implicits.global

class GoalAlertsHealthcheckPage extends ManagementPage {
  val HealthCheckWait = 200.millis

  override val path = "/management/healthcheck"

  override def get(req: HttpRequest) = PlainTextResponse("OK")

}
