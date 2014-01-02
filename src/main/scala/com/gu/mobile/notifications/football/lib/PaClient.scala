package com.gu.mobile.notifications.football.lib

import pa._
import scala.concurrent.{ExecutionContext, Future}
import dispatch._
import com.gu.mobile.notifications.football.conf.GoalNotificationsConfig
import com.gu.mobile.notifications.football.management.Metrics
import com.gu.mobile.notifications.football.lib.Futures._

trait DispatchHttp extends pa.Http {
  def GET(urlString: String): Future[Response] = {
    /** This unfortunately has to be done here due to how pa.Http's interface is written */
    import ExecutionContext.Implicits.global

    dispatch.Http(url(urlString) OK as.Response(r => Response(r.getStatusCode, r.getResponseBody, r.getStatusText)))
  }
}

object PaFootballClient extends PaClient with DispatchHttp {
  def apiKey: String = GoalNotificationsConfig.paApiKey

  override protected def get(suffix: String)(implicit context: ExecutionContext): Future[String] = {
    val ftr = super.get(suffix)(context)
    ftr.recordTimeSpent(Metrics.paResponseTime, Metrics.paErrorResponseTime)
    ftr
  }
}
