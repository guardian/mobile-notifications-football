package com.gu.mobile.notifications.football.management

import com.gu.management.internal.{ManagementServer, ManagementHandler}
import com.gu.management.{StatusPage, ManifestPage, ManagementPage}

object MobileNotificationsManagementServer {
  private val handler = new ManagementHandler {
    val applicationName: String = "mobile-notifications-football"

    def pages: List[ManagementPage] = List(
      new ManifestPage(),
      new StatusPage(applicationName, () => Metrics.all)
    )
  }

  def start() = ManagementServer.start(handler)

  def stop() = ManagementServer.shutdown()
}
