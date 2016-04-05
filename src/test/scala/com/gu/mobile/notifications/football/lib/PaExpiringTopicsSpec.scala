package com.gu.mobile.notifications.football.lib

import com.gu.mobile.notifications.client.models.{Topic, TopicTypes}
import org.scalatest.{Matchers, WordSpec}
import pa._

import scala.concurrent.{Await, ExecutionContext, Future}
import ExecutionContext.Implicits.global
import pa.MatchDay
import pa.Response

import scala.concurrent.duration.Duration
import com.gu.mobile.notifications.football.helpers.EmptyInstances

class PaExpiringTopicsSpec extends WordSpec with Matchers with EmptyInstances {
  trait NoHttp extends Http {
    override def GET(url: String): Future[Response] = Future.failed(new RuntimeException("This is a fixture HTTP only"))
  }

  def matchInfoFixtureClient(matchDays: List[MatchDay]) = {
    PaExpiringTopics(new PaClient with NoHttp {
      val apiKey = ""

      override def matchInfo(id: String)(implicit context: ExecutionContext) = {
        matchDays.find(_.id == id) match {
          case Some(matchDay) => Future.successful(matchDay)
          case None => Future.failed(new RuntimeException(s"Could not find match day with id $id"))
        }
      }
    })
  }

  "PaExpiringTopics" should {
    "return a list of topics for which there were FT statuses" in {
      val fixtureClient = matchInfoFixtureClient(List(
        MatchDay.forIdAndStatus("1", "FT"),
        MatchDay.forIdAndStatus("2", "KO"),
        MatchDay.forIdAndStatus("3", "Abandoned"),
        MatchDay.forIdAndStatus("4", "HT"),
        MatchDay.forIdAndStatus("5", "Cancelled")
      ))

      val topic1 = Topic(
        TopicTypes.FootballMatch,
        "1"
      )
      val topic2 = Topic(
        TopicTypes.FootballMatch,
        "2"
      )
      val topic3 = Topic(
        TopicTypes.FootballMatch,
        "3"
      )
      val topic4 = Topic(
        TopicTypes.FootballMatch,
        "4"
      )
      val topic5 = Topic(
        TopicTypes.FootballMatch,
        "5"
      )
      val topic6 = Topic(
        TopicTypes.FootballMatch,
        "6"
      )

      Await.result(fixtureClient.getExpired(List(
        topic1,
        topic2,
        topic3,
        topic4,
        topic5,
        topic6
      )), Duration.Inf) should be(List(
        topic1,
        topic3,
        topic5
      ))
    }
  }
}
