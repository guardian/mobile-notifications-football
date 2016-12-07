package com.gu.mobile.notifications.football.lib

import pa._
import scala.concurrent.{ExecutionContext, Future}
import dispatch._
import com.gu.mobile.notifications.football.Configuration
import grizzled.slf4j.Logging

trait DispatchHttp extends pa.Http with Logging {

  import ExecutionContext.Implicits.global

  def GET(urlString: String): Future[Response] = {
    logger.info("Http GET " + urlString.replaceAll(Configuration.paApiKey, "<api-key>"))
    dispatch.Http(url(urlString) OK as.Response(r => Response(r.getStatusCode, r.getResponseBody, r.getStatusText)))
  }
}

object PaFootballClient extends PaClient with DispatchHttp {
  def apiKey = Configuration.paApiKey
  override lazy val base: String = Configuration.paHost

  override protected def get(suffix: String)(implicit context: ExecutionContext): Future[String] = super.get(suffix)(context)
}
