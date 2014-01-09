package com.gu.mobile.notifications.football.conf

import com.gu.conf.ConfigurationFactory
import com.amazonaws.auth.BasicAWSCredentials

object GoalNotificationsConfig {
  private lazy val configuration = ConfigurationFactory(
    "mobile-notifications-football", "conf/mobile-notifications-football"
  )

  lazy val paApiKey = configuration("pa.api_key")

  lazy val guardianNotificationsHost = configuration("notifications.host")

  lazy val dynamoDbReadOnly = new BasicAWSCredentials(
    configuration.getStringProperty("credentials.dynamodb.access_key", ""),
    configuration.getStringProperty("credentials.dynamodb.secret_key", ""))

  lazy val dynamoDbEndpoint = configuration("dynamodb.endpoint")

  lazy val dynamoDbSwitchesTableName = configuration("dynamodb.switches_table")
}
