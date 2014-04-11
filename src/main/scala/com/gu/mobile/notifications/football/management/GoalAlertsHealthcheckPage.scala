package com.gu.mobile.notifications.football.management

import com.gu.management._
import com.gu.mobile.notifications.football.GoalNotificationsPipeline
import scala.concurrent.duration._
import scala.concurrent.Await
import com.gu.mobile.notifications.client.models.{Unhealthy, Ok}
import com.gu.management.{HttpRequest, PlainTextResponse}

class GoalAlertsHealthcheckPage extends ManagementPage {
  val HealthCheckWait = 200.millis

  override val path = "/management/healthcheck"

  override def get(req: HttpRequest): Response = {
    /** TODO this sucks - fix the API for ManagementPages so it will accept Futures */
    Await.result(GoalNotificationsPipeline.healthcheck, HealthCheckWait) match {
      case Ok => PlainTextResponse("OK")
      case Unhealthy(errorCode) => ErrorResponse(errorCode, "Unable to contact push API")
    }
  }
}
