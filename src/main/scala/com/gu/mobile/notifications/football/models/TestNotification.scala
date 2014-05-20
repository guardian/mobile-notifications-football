package com.gu.mobile.notifications.football.models

import com.gu.mobile.notifications.football.lib.GoalNotificationBuilder

object TestNotification {
  def get = {
    val homeTeam = MatchEventTeam(
      12, "Man Utd"
    )

    val awayTeam = MatchEventTeam(
      4, "Chelsea"
    )

    GoalNotificationBuilder(Goal(
      "Bobby Charlton",
      homeTeam,
      awayTeam,
      64,
      None
    ), EventFeedMetadata(
      "1234",
      homeTeam,
      2,
      awayTeam,
      1
    ))
  }
}
