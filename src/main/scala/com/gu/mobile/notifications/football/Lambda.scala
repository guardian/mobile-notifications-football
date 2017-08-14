package com.gu.mobile.notifications.football

import com.amazonaws.regions.Regions._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.lambda.runtime.Context
import com.gu.mobile.notifications.football.lib._
import com.gu.Logging
import com.gu.mobile.notifications.client.ApiClient
import com.gu.mobile.notifications.football.notificationbuilders.{GoalNotificationBuilder, MatchStatusNotificationBuilder}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong

class LambdaInput()

object Lambda extends App with Logging {

  var cachedLambda = null.asInstanceOf[Boolean]

  def tableName = s"mobile-notifications-football-${configuration.stage}"

  lazy val configuration = {
    logger.debug("Creating configuration")
    new Configuration()
  }

  lazy val paFootballClient = {
    logger.debug("Creating pa football client")
    new PaFootballClient(configuration.paApiKey, configuration.paHost)
  }

  lazy val dynamoDBClient: AmazonDynamoDBAsyncClient = {
    logger.debug("Creating dynamo db client")
    new AmazonDynamoDBAsyncClient(configuration.credentials).withRegion(EU_WEST_1)
  }

  lazy val syntheticMatchEventGenerator = new SyntheticMatchEventGenerator()

  lazy val goalNotificationBuilder = new GoalNotificationBuilder(configuration.mapiHost)

  lazy val notificationHttpProvider = new NotificationHttpProvider()

  lazy val notificationClient = ApiClient(
    host = configuration.notificationsHost,
    apiKey = configuration.notificationsApiKey,
    httpProvider = notificationHttpProvider,
    legacyHost = configuration.notificationsLegacyHost,
    legacyApiKey = configuration.notificationsLegacyApiKey
  )

  lazy val matchStatusNotificationBuilder = new MatchStatusNotificationBuilder(configuration.mapiHost)

  lazy val eventConsumer = new EventConsumerImpl(goalNotificationBuilder, matchStatusNotificationBuilder, notificationClient)

  lazy val distinctCheck = new DynamoDistinctCheck(dynamoDBClient, tableName)

  lazy val footballActor = {
    logger.debug("Creating actor")
    new PaFootballActor(paFootballClient, distinctCheck, syntheticMatchEventGenerator, eventConsumer)
  }

  def handler(lambdaInput: LambdaInput, context: Context): String = {
    if (cachedLambda) {
      logger.info("Re-using existing container")
    } else {
      cachedLambda = true
      logger.info("Starting new container")
    }

    Await.ready(footballActor.start, 35.seconds)
    "done"
  }

}