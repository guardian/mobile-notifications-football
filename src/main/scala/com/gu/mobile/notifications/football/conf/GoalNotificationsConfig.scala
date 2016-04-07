package com.gu.mobile.notifications.football.conf

import com.gu.conf.ConfigurationFactory
import com.amazonaws.auth.BasicAWSCredentials

object GoalNotificationsConfig {
  private lazy val configuration = ConfigurationFactory(
    "mobile-notifications-football", "conf/mobile-notifications-football"
  )

  lazy val mapiFootballHost = configuration("mobile_apps_api.football_host")

  lazy val paApiKey = configuration("pa.api_key")

  lazy val guardianNotificationsHost = configuration("notifications.client.host")
  lazy val guardianNotificationsApiKey = configuration("notifications.client.api.key")
  lazy val guardianNotificationsLegacyHost = configuration("notifications.client.legacy.host")
  lazy val guardianNotificationsLegacyApiKey = configuration("notifications.client.legacy.api.key")

  lazy val snsAccessKey = configuration.getStringProperty("credentials.sns.access_key")
  lazy val snsSecretKey = configuration.getStringProperty("credentials.sns.secret_key")
  lazy val snsEndpoint = configuration.getStringProperty("sns.endpoint")
  lazy val snsTopic = configuration.getStringProperty("sns.topic")

  lazy val snsQueuePublishCredentials = for {
    accessKey <- snsAccessKey
    secretKey <- snsSecretKey
  } yield new BasicAWSCredentials(accessKey, secretKey)
}
