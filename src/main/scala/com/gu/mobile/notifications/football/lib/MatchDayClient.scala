package com.gu.mobile.notifications.football.lib

import pa.{MatchDay, MatchEvents, PaClient}

import scala.concurrent.Future
import org.joda.time.{DateTime, LocalDate}

import scala.concurrent.ExecutionContext.Implicits.global

trait MatchDayClient {
  /** Today's match day */
  def today: Future[List[MatchDay]]

  def matchEvents(id: String): Future[MatchEvents]
}

case class PaMatchDayClient(api: PaClient) extends MatchDayClient {
  def aroundToday: Future[List[MatchDay]] = {
    val today = DateTime.now.toLocalDate
    val yesterday = today.minusDays(1)
    val tomorrow = today.plusDays(1)

    val days = List(yesterday, today, tomorrow)

    Future.reduce(days.map(api.matchDay))(_ ++ _)
  }
  def today = api.matchDay(DateTime.now.toLocalDate)

  /** It's a bit weird that our PA client returns None for broken match event feeds (see the client for more details)
    * rather than encapsulating the error as a failed Future. This cleans that up.
    */
  def matchEvents(id: String): Future[MatchEvents] = api.matchEvents(id) map {
    _.getOrElse(throw new RuntimeException(s"Broken match feed for $id"))
  }
}
