package com.gu.mobile.notifications.football.lib

import pa.{MatchEvents, PaClient, MatchDay}
import scala.concurrent.Future
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext.Implicits.global

trait MatchDayClient {
  /** Today's match day */
  def today: Future[List[MatchDay]]

  def matchEvents(id: String): Future[MatchEvents]
}

case class PaMatchDayClient(api: PaClient) extends MatchDayClient {
  def today = api.matchDay(DateTime.now.toDateMidnight)

  /** It's a bit weird that our PA client returns None for broken match event feeds (see the client for more details)
    * rather than encapsulating the error as a failed Future. This cleans that up.
    */
  def matchEvents(id: String) = api.matchEvents(id) map {
    _.getOrElse(throw new RuntimeException(s"Broken match feed for $id"))
  }
}