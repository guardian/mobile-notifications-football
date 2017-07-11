package com.gu.mobile.notifications.football

import java.net.URL

import com.amazonaws.regions.Regions._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.lambda.runtime.Context
import com.gu.football.PaFootballActor
import com.gu.mobile.notifications.football.lib.{GoalNotificationBuilder, NotificationHttpProvider, PaFootballClient, PaMatchDayClient}
import com.gu.mobile.notifications.client.ApiClient
import com.gu.Logging
import org.joda.time.{DateTime, DateTimeUtils}
import play.api.libs.json.Json

import scala.beans.BeanProperty
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * This is compatible with aws' lambda JSON to POJO conversion
  */
class LambdaInput() {
  @BeanProperty var name: String = _
}

object Lambda extends App with Logging {

  var cachedLambda = null.asInstanceOf[Boolean]

  def tableName = s"mobile-notifications-football-${configuration.stage}"

  lazy val configuration = {
    logger.debug("Creating configuration")
    new Configuration()
  }

  lazy val paMatchDayClient = {
    logger.debug("Creating pa match day client")
    PaMatchDayClient(new PaFootballClient(configuration.paApiKey, configuration.paHost))
  }

  lazy val dynamoDBClient: AmazonDynamoDBAsyncClient = {
    logger.debug("Creating dynamo db client")
    new AmazonDynamoDBAsyncClient(configuration.credentials).withRegion(EU_WEST_1)
  }

  lazy val goalNotificationBuilder = new GoalNotificationBuilder(configuration.mapiHost)

  lazy val notificationHttpProvider = {
    new NotificationHttpProvider()
  }

  lazy val notificationClient = ApiClient(
    host = configuration.notificationsHost,
    apiKey = configuration.notificationsApiKey,
    httpProvider = notificationHttpProvider,
    legacyHost = configuration.notificationsLegacyHost,
    legacyApiKey = configuration.notificationsLegacyApiKey
  )

  lazy val footballActor = new PaFootballActor(paMatchDayClient, dynamoDBClient, tableName, goalNotificationBuilder, notificationClient)

  def debugSetTime(): Unit = {
    // this is only used to debug and should never reach master nor prod
    if (configuration.stage == "CODE") {
      val is = new URL("https://hdjq4n85yi.execute-api.eu-west-1.amazonaws.com/Prod/getTime").openStream()
      val json = Json.parse(Source.fromInputStream(is).mkString)
      val date = DateTime.parse((json \ "currentDate").as[String])
      logger.info(s"Force the date to $date")
      DateTimeUtils.setCurrentMillisFixed(date.getMillis)
    }
  }

  def handler(lambdaInput: LambdaInput, context: Context): String = {
    debugSetTime()
    if (cachedLambda) {
      logger.info("Re-using existing container")
    } else {
      cachedLambda = true
      logger.info("Starting new container")
    }
    Await.ready(footballActor.start, 35.seconds)
    "done"
  }

  override def main(args: Array[String]): Unit = {
    handler(null, null)
  }

}