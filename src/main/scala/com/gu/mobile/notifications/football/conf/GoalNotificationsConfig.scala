package com.gu.mobile.notifications.football.conf

import com.gu.conf.ConfigurationFactory
import com.amazonaws.auth.BasicAWSCredentials

object GoalNotificationsConfig {
  private lazy val configuration = ConfigurationFactory(
    "mobile-notifications-football", "conf/mobile-notifications-football"
  )

  lazy val mapiFootballHost = configuration("mobile_apps_api.football_host")

  lazy val paApiKey = configuration("pa.api_key")

  lazy val guardianNotificationsHost = configuration("notifications.host")

  lazy val snsAccessKey = configuration.getStringProperty("credentials.sns.access_key")
  lazy val snsSecretKey = configuration.getStringProperty("credentials.sns.secret_key")
  lazy val snsEndpoint = configuration.getStringProperty("sns.endpoint")
  lazy val snsTopic = configuration.getStringProperty("sns.topic")
  lazy val goalAlertsApiKey = configuration.getStringProperty("goal.alerts.api.key")

  lazy val snsQueuePublishCredentials = for {
    accessKey <- snsAccessKey
    secretKey <- snsSecretKey
  } yield new BasicAWSCredentials(accessKey, secretKey)
}
