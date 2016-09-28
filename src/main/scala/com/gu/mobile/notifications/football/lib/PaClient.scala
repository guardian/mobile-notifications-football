package com.gu.mobile.notifications.football.lib

import pa._
import scala.concurrent.{ExecutionContext, Future}
import dispatch._
import com.gu.mobile.notifications.football.conf.GoalNotificationsConfig
import grizzled.slf4j.Logging

trait DispatchHttp extends pa.Http with Logging {
  def GET(urlString: String): Future[Response] = {
    import ExecutionContext.Implicits.global

    logger.debug("Http GET " + urlString.replaceAll(GoalNotificationsConfig.paApiKey, "<api-key>"))
    dispatch.Http(url(urlString) OK as.Response(r => Response(r.getStatusCode, r.getResponseBody, r.getStatusText)))
  }
}

object PaFootballClient extends PaClient with DispatchHttp {
  def apiKey = GoalNotificationsConfig.paApiKey
  override lazy val base: String = GoalNotificationsConfig.paHost

  override protected def get(suffix: String)(implicit context: ExecutionContext): Future[String] = super.get(suffix)(context)
}
