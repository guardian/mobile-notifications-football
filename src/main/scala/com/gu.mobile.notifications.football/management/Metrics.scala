package com.gu.mobile.notifications.football.management

import com.gu.management.{Metric, TimingMetric}

object Metrics {
  /** TODO define some metrics */
  val paResponseTime = new TimingMetric(
    "response_times",
    "pa",
    "PA API response times",
    "PA API response times"
  )

  val paErrorResponseTime = new TimingMetric(
    "error_response_times",
    "pa",
    "PA API error response times",
    "PA API error response times"
  )

  val notificationsResponseTime = new TimingMetric(
    "response_times",
    "notifications",
    "Mobile Notifications response times",
    "Mobile Notifications response times"
  )

  val notificationsErrorResponseTime = new TimingMetric(
    "error_response_times",
    "notifications",
    "Mobile Notifications error response times",
    "Mobile Notifications error response times"
  )

  val all: Seq[Metric] = Seq(
    paResponseTime,
    paErrorResponseTime,
    notificationsResponseTime,
    notificationsErrorResponseTime
  )
}
