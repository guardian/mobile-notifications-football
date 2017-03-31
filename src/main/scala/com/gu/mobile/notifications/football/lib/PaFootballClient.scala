package com.gu.mobile.notifications.football.lib

import pa._

import scala.concurrent.{ExecutionContext, Future}
import dispatch._
import grizzled.slf4j.Logging
import org.joda.time.DateTime

class PaFootballClient(override val apiKey: String, apiBase: String) extends PaClient with pa.Http with Logging {

  import ExecutionContext.Implicits.global

  override lazy val base = apiBase

  def GET(urlString: String): Future[Response] = {
    logger.info("Http GET " + urlString.replaceAll(apiKey, "<api-key>"))
    dispatch.Http(url(urlString) OK as.Response(r => Response(r.getStatusCode, r.getResponseBody, r.getStatusText)))
  }

  override protected def get(suffix: String)(implicit context: ExecutionContext): Future[String] = super.get(suffix)(context)

  def aroundToday: Future[List[MatchDay]] = {
    val today = DateTime.now.toLocalDate
    val yesterday = today.minusDays(1)
    val tomorrow = today.plusDays(1)

    val days = List(yesterday, today, tomorrow)

    Future.reduce(days.map { day => matchDay(day).recover { case _ => List.empty } })(_ ++ _)
  }

  def eventsForMatch(id: String, syntheticMatchEventGenerator: SyntheticMatchEventGenerator)(implicit ec: ExecutionContext): Future[List[MatchEvent]] =
    (for {
      info <- matchInfo(id)
      events <- matchEvents(id).map(_.toList.flatMap(_.events))
    } yield {
      syntheticMatchEventGenerator.generate(events, id, info)
    }) recover {
      case e =>
        logger.error(s"Failed to fetch events for match $id: ${e.getMessage}")
        List.empty
    }
}
