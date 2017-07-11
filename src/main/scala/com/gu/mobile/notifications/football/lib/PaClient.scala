package com.gu.mobile.notifications.football.lib

import pa._

import scala.concurrent.{ExecutionContext, Future}
import grizzled.slf4j.Logging
import okhttp3.{OkHttpClient, Request}

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
  override lazy val base = apiBase

  override protected def get(suffix: String)(implicit context: ExecutionContext): Future[String] = super.get(suffix)(context)
}
