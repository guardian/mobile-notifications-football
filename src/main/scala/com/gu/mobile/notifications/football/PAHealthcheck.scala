package com.gu.mobile.notifications.football

import java.util.concurrent.atomic.AtomicBoolean
import com.gu.mobile.notifications.football.lib.PaFootballClient
import grizzled.slf4j.Logging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object PAHealthCheck extends Logging {
  import SystemSetup.system
  implicit val ec: ExecutionContext = system.dispatcher

  private val _contactedPaSuccessfully = new AtomicBoolean(true)

  def startRefreshCycle(): Unit = system.scheduler.schedule(0 seconds, 3 minutes) {
    logger.info("Refreshing PA healthcheck")
    val BarcelonaTeamId = "26300"
    PaFootballClient.squad(BarcelonaTeamId)
      .onComplete {
        case Success(_) =>
          logger.info("PA Healthcheck: [healthy]")
          _contactedPaSuccessfully.set(true)
        case Failure(_) =>
          logger.info("PA Healthcheck: [NOT HEALTHY]")
          _contactedPaSuccessfully.set(false)
      }
  }

  def contactedPaSuccessfully: Boolean = _contactedPaSuccessfully.get
}
