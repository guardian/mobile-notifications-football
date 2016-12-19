package com.gu.mobile.notifications.football.conf

import com.gu.conf.{Configuration, ConfigurationFactory}
import com.amazonaws.auth.BasicAWSCredentials

object GoalNotificationsConfig {

  implicit class RichConfiguration(config: Configuration) {
    def getBooleanProperty(key: String, default: Boolean = false): Boolean =
      config.getStringProperty(key) map {
        _.toLowerCase match {
          case "true" | "yes" | "on" => true
          case "false" | "no" | "off" => false
          case _ => default
        }
      } getOrElse default
  }

  private lazy val configuration = ConfigurationFactory(
    "mobile-notifications-football", "conf/mobile-notifications-football"
  )

  lazy val mapiHost = configuration("mobile_apps_api.host")

  lazy val paApiKey = configuration("pa.api_key")
  lazy val paHost = configuration.getStringProperty("pa.host", "https://football.api.press.net/v1.5")

  lazy val guardianNotificationsHost = configuration("notifications.client.host")
  lazy val guardianNotificationsApiKey = configuration("notifications.client.api.key")
  lazy val guardianNotificationsLegacyHost = configuration("notifications.client.legacy.host")
  lazy val guardianNotificationsLegacyApiKey = configuration("notifications.client.legacy.api.key")

  lazy val snsAccessKey = configuration.getStringProperty("credentials.sns.access_key")
  lazy val snsSecretKey = configuration.getStringProperty("credentials.sns.secret_key")
  lazy val snsEndpoint = configuration.getStringProperty("sns.endpoint")
  lazy val snsTopic = configuration.getStringProperty("sns.topic")
  lazy val goalAlertsApiKey = configuration.getStringProperty("goal.alerts.api.key")
  lazy val testEndPointEnabled = configuration.getBooleanProperty("test_endpoint_enabled", false)

  lazy val pollingEnabled = configuration.getBooleanProperty("polling_enabled", true)
  lazy val snsQueuePublishCredentials = for {
    accessKey <- snsAccessKey
    secretKey <- snsSecretKey
  } yield new BasicAWSCredentials(accessKey, secretKey)
}
