package com.gu.mobile.notifications.football.lib

import com.gu.mobile.notifications.football.models.MatchId
import pa._

import scala.concurrent.{ExecutionContext, Future}
import grizzled.slf4j.Logging
import okhttp3.{OkHttpClient, Request}
import org.joda.time.DateTime

trait OkHttp extends pa.Http with Logging {

  val httpClient = new OkHttpClient

  def apiKey: String

  def GET(urlString: String): Future[Response] = {
    logger.info("Http GET " + urlString.replaceAll(apiKey, "<api-key>"))
    val httpRequest = new Request.Builder().url(urlString).build()
    val httpResponse = httpClient.newCall(httpRequest).execute()
    Future.successful(Response(httpResponse.code(), httpResponse.body().string, httpResponse.message()))
  }
}

class PaFootballClient(override val apiKey: String, apiBase: String) extends PaClient with OkHttp {

  import ExecutionContext.Implicits.global

  override lazy val base = apiBase

  override protected def get(suffix: String)(implicit context: ExecutionContext): Future[String] = super.get(suffix)(context)

  def aroundToday: Future[List[MatchDay]] = {
    val today = DateTime.now.toLocalDate
    val yesterday = today.minusDays(1)
    val tomorrow = today.plusDays(1)

    val days = List(yesterday, today, tomorrow)

    Future.reduce(days.map { day => matchDay(day).recover { case _ => List.empty } })(_ ++ _)
  }

  def eventsForMatch(matchId: MatchId, syntheticMatchEventGenerator: SyntheticMatchEventGenerator)(implicit ec: ExecutionContext): Future[(MatchDay, List[MatchEvent])] =
    for {
      matchDay <- matchInfo(matchId.id)
      events <- matchEvents(matchId.id).map(_.toList.flatMap(_.events))
    } yield {
      (matchDay, syntheticMatchEventGenerator.generate(events, matchId.id, matchDay))
    }
}
