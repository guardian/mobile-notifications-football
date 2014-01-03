package com.gu.mobile.notifications.football.lib

import pa.{PaClient, MatchDay}
import scala.concurrent.Future
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext.Implicits.global
import com.gu.mobile.notifications.football.lib.Observables._
import rx.lang.scala.Observable

trait MatchDayClient {
  /** Today's match day */
  def today: Future[List[MatchDay]]

  def observable = Observable repeatedly today
}

case class PaMatchDayClient(api: PaClient) extends MatchDayClient {
  def today = api.matchDay(DateTime.now.toDateMidnight)
}