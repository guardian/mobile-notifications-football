package com.gu.mobile.notifications.football

import java.net.URL

import com.amazonaws.regions.Regions._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.lambda.runtime.Context
import com.gu.mobile.notifications.football.lib._
import com.gu.Logging
import com.gu.mobile.notifications.client.ApiClient
import com.gu.mobile.notifications.football.notificationbuilders.{GoalNotificationBuilder, MatchStatusNotificationBuilder}
import org.joda.time.{DateTime, DateTimeUtils}
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong
import scala.io.Source

class LambdaInput()

object Lambda extends App with Logging {

  var cachedLambda = null.asInstanceOf[Boolean]

  def tableName = s"mobile-notifications-football-events-${configuration.stage}"

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

  lazy val notificationSender = new NotificationSender(notificationClient)

  lazy val matchStatusNotificationBuilder = new MatchStatusNotificationBuilder(configuration.mapiHost)

  lazy val eventConsumer = new EventConsumer(goalNotificationBuilder, matchStatusNotificationBuilder)

  lazy val distinctCheck = new DynamoDistinctCheck(dynamoDBClient, tableName)

  lazy val eventFilter = new CachedEventFilter(distinctCheck)

  lazy val footballData = new FootballData(paFootballClient, eventFilter, syntheticMatchEventGenerator, eventConsumer)

  def debugSetTime(): Unit = {
    // this is only used to debug
    if (configuration.stage == "CODE") {
      val is = new URL("https://hdjq4n85yi.execute-api.eu-west-1.amazonaws.com/Prod/getTime").openStream()
      val json = Json.parse(Source.fromInputStream(is).mkString)
      val date = DateTime.parse((json \ "currentDate").as[String])
      logger.info(s"Force the date to $date")
      DateTimeUtils.setCurrentMillisFixed(date.getMillis)
    }
  }

  private def logContainer() = {
    if (cachedLambda) {
      logger.info("Re-using existing container")
    } else {
      cachedLambda = true
      logger.info("Starting new container")
    }
  }

  def handler(lambdaInput: LambdaInput, context: Context): String = {
    debugSetTime()
    logContainer()

    val result = footballData.pollFootballData
      .map(_.flatMap(eventConsumer.receiveEvents))
      .flatMap(notificationSender.sendNotifications)

    Await.ready(result, 35.seconds)
    "done"
  }

}
