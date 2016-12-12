package com.gu.mobile.notifications.football.lib

import pa._
import scala.concurrent.{ExecutionContext, Future}
import dispatch._
import com.gu.mobile.notifications.football.Configuration
import grizzled.slf4j.Logging

trait DispatchHttp extends pa.Http with Logging {

  import ExecutionContext.Implicits.global

  def apiKey: String

  def GET(urlString: String): Future[Response] = {
    logger.info("Http GET " + urlString.replaceAll(apiKey, "<api-key>"))
    dispatch.Http(url(urlString) OK as.Response(r => Response(r.getStatusCode, r.getResponseBody, r.getStatusText)))
  }
}

class PaFootballClient(override val apiKey: String, apiBase: String) extends PaClient with DispatchHttp {
  override lazy val base = apiBase

  override protected def get(suffix: String)(implicit context: ExecutionContext): Future[String] = super.get(suffix)(context)
}
