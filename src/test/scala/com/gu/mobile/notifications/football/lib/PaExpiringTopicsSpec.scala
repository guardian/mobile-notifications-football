package com.gu.mobile.notifications.football.lib

import org.scalatest.{Matchers, WordSpec}
import pa._
import scala.concurrent.{ExecutionContext, Future, Await}
import ExecutionContext.Implicits.global
import org.joda.time.DateTime
import pa.MatchDay
import pa.Response
import com.gu.mobile.notifications.client.models.legacy.Topic
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
        "football-match",
        "1"
      )
      val topic2 = Topic(
        "football-match",
        "2"
      )
      val topic3 = Topic(
        "football-match",
        "3"
      )
      val topic4 = Topic(
        "football-match",
        "4"
      )
      val topic5 = Topic(
        "football-match",
        "5"
      )
      val topic6 = Topic(
        "football-match",
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
