package com.gu.mobile.notifications.football

import akka.actor.{ActorSystem, Props}
import com.amazonaws.regions.Regions._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.lambda.runtime.Context
import com.gu.football.PaFootballActor
import com.gu.football.PaFootballActor.TriggerPoll
import com.gu.mobile.notifications.football.lib.{NotificationHttpProvider, PaFootballClient, PaMatchDayClient}
import akka.pattern.ask
import akka.util.Timeout
import com.gu.mobile.notifications.client.ApiClient
import grizzled.slf4j.Logging

import scala.beans.BeanProperty
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * This is compatible with aws' lambda JSON to POJO conversion
  */
class LambdaInput() {
  @BeanProperty var name: String = _
}

object Lambda extends App with Logging {

  var cachedLambda = null.asInstanceOf[Boolean]

  def tableName = s"mobile-notifications-football-${configuration.stage}"

  implicit lazy val system = {
    logger.debug("Creating actor system")
    ActorSystem("mobile-notification-football")
  }

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

  lazy val footballActor = {
    logger.debug("Creating actor")
    system.actorOf(
      Props(classOf[PaFootballActor], paMatchDayClient, dynamoDBClient, tableName),
      "goal-notifications-actor-solution"
    )
  }

  lazy val notificationHttpProvider = {
    implicit val ec = system.dispatcher
    new NotificationHttpProvider()
  }

  lazy val notificationClient = ApiClient(
    host = configuration.notificationsHost,
    apiKey = configuration.notificationsApiKey,
    httpProvider = notificationHttpProvider,
    legacyHost = configuration.notificationsLegacyHost,
    legacyApiKey = configuration.notificationsLegacyApiKey
  )

  def handler(lambdaInput: LambdaInput, context: Context): String = {
    if (cachedLambda) {
      logger.info("Re-using existing container")
    } else {
      cachedLambda = true
      logger.info("Starting new container")
    }

    implicit val timeout = Timeout(30.seconds)
    val done = footballActor ? TriggerPoll
    Await.ready(done, 35.seconds)
    "done"
  }

}