package com.gu.mobile.notifications.football.lib

import pa.{PaClient, MatchDay}
import scala.concurrent.Future
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext.Implicits.global

trait MatchDayClient {
  /** Today's match day */
  def today: Future[List[MatchDay]]
}

case class PaMatchDayClient(api: PaClient) extends MatchDayClient {
  def today = api.matchDay(DateTime.now.toDateMidnight)
}