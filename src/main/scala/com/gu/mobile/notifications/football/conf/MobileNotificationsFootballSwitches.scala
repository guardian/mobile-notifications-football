package com.gu.mobile.notifications.football.conf

import com.gu.dynamodbswitches.{Switch, Switches}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient

object MobileNotificationsFootballSwitches extends Switches {
  def dynamoDbClient: AmazonDynamoDBClient = {
    val client = new AmazonDynamoDBClient(GoalNotificationsConfig.dynamoDbReadOnly)
    client.setEndpoint(GoalNotificationsConfig.dynamoDbEndpoint)
    client
  }

  override val dynamoDbTableName = GoalNotificationsConfig.dynamoDbSwitchesTableName

  val sendNotifications = Switch("goal_notifications", default = true)

  val all: List[Switch] = List(sendNotifications)
}
