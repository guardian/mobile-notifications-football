package com.gu.mobile.notifications.football.lib

import pa._
import scala.concurrent.{ExecutionContext, Future}
import dispatch._
import com.gu.mobile.notifications.football.conf.GoalNotificationsConfig

trait DispatchHttp extends pa.Http {
  implicit val executionContext: ExecutionContext

  def GET(urlString: String): Future[Response] = {
    dispatch.Http(url(urlString) OK as.Response(r => Response(r.getStatusCode, r.getResponseBody, r.getStatusText)))
  }
}

object PaFootballClient extends PaClient with DispatchHttp {
  /** TODO: use the dispatcher from the actor system instead? */
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def apiKey: String = GoalNotificationsConfig.paApiKey
}
