package com.gu.mobile.notifications.football.conf

import com.gu.conf.ConfigurationFactory

object GoalNotificationsConfig {
  private lazy val configuration = ConfigurationFactory(
    "mobile-notifications-football", "conf/mobile-notifications-football"
  )

  lazy val paApiKey = configuration("pa.api_key")
}
