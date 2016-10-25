package com.gu.mobile.notifications.football.lib

import pa._
import scala.concurrent.{ExecutionContext, Future}
import dispatch._
import com.gu.mobile.notifications.football.conf.GoalNotificationsConfig
import grizzled.slf4j.Logging
import spray.caching.{LruCache, Cache}
import scala.concurrent.duration._

trait DispatchHttp extends pa.Http with Logging {

  val cache: Cache[Response] = LruCache(timeToLive = 1.minute)

  import ExecutionContext.Implicits.global

  def GET(urlString: String): Future[Response] = cache(urlString) {
    logger.info("Http GET " + urlString.replaceAll(GoalNotificationsConfig.paApiKey, "<api-key>"))
    dispatch.Http(url(urlString) OK as.Response(r => Response(r.getStatusCode, r.getResponseBody, r.getStatusText)))
  }
}

object PaFootballClient extends PaClient with DispatchHttp {
  def apiKey = GoalNotificationsConfig.paApiKey
  override lazy val base: String = GoalNotificationsConfig.paHost

  override protected def get(suffix: String)(implicit context: ExecutionContext): Future[String] = super.get(suffix)(context)
}
