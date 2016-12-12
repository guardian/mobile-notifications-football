package com.gu.mobile.notifications.football

import akka.actor.{ActorSystem, Props}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.regions.Regions._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.lambda.runtime.Context
import com.gu.football.PaFootballActor
import com.gu.football.PaFootballActor.TriggerPoll
import com.gu.mobile.notifications.football.lib.{PaFootballClient, PaMatchDayClient}
import akka.pattern.ask
import akka.util.Timeout

import scala.beans.BeanProperty
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * This is compatible with aws' lambda JSON to POJO conversion
  */
class LambdaInput() {
  @BeanProperty var name: String = _
}

object Lambda extends App {

  var globalContext: Option[Context] = None

  lazy val tableName = {
    println("Initialising table name")
    "mobile-notifications-football-CODE"
  }

  implicit lazy val system = {
    globalContext.foreach { _.getLogger.log("Creating actor system\n")}
    ActorSystem("goal-notifications-system")
  }

  lazy val configuration = {
    globalContext.foreach { _.getLogger.log("Creating configuration\n")}
    new Configuration()
  }

  lazy val paMatchDayClient = {
    globalContext.foreach { _.getLogger.log("Creating pa match day client\n")}
    PaMatchDayClient(new PaFootballClient(configuration.paApiKey, configuration.paHost))
  }

  lazy val dynamoDBClient: AmazonDynamoDBAsyncClient = {
    globalContext.foreach { _.getLogger.log("Creating dynamo db client\n")}
    new AmazonDynamoDBAsyncClient(configuration.credentials).withRegion(EU_WEST_1)
  }

  lazy val footballActor = {
    globalContext.foreach { _.getLogger.log("Creating actor\n")}
    system.actorOf(
      Props(classOf[PaFootballActor], paMatchDayClient, dynamoDBClient, tableName),
      "goal-notifications-actor-solution"
    )
  }

  def handler(lambdaInput: LambdaInput, context: Context): String = {
    globalContext = Some(context)
    implicit val timeout = Timeout(60.seconds)
    val done = footballActor ? TriggerPoll(context.getLogger)
    Await.ready(done, 60.seconds)
    globalContext = None
    "done"
  }
}