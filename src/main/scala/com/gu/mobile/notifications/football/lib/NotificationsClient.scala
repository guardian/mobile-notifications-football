package com.gu.mobile.notifications.football.lib

import com.gu.mobile.notifications.client.models.GoalAlertPayload
import com.gu.mobile.notifications.client._
import dispatch._

import scala.concurrent.Future
import com.gu.mobile.notifications.football.conf.GoalNotificationsConfig
import com.gu.mobile.notifications.football.lib.Futures._
import com.gu.mobile.notifications.football.management.Metrics
import grizzled.slf4j.Logging

import scala.concurrent.ExecutionContext.Implicits.global

object NotificationHttpProvider extends HttpProvider with Logging {
  override def get(urlString: String): Future[HttpResponse] = execute(url(urlString))

  override def post(urlString: String, contentType: ContentType, body: Array[Byte]): Future[HttpResponse] = {
    val ftr = execute(url(urlString)
      .setMethod("POST")
      .setContentType(contentType.mediaType, contentType.charset)
      .setBody(body)
    )
    ftr.recordTimeSpent(Metrics.notificationsResponseTime, Metrics.notificationsErrorResponseTime)

    ftr onFailure {
      case error => logger.error("Error trying to send notification", error)
    }

    ftr
  }

  private def execute(request: Req) = Http(request).map { response =>
    if (response.getStatusCode >= 200 && response.getStatusCode < 300) {
      HttpOk(response.getStatusCode, response.getResponseBody)
    } else {
      HttpError(response.getStatusCode, response.getResponseBody)
    }
  }
}

class NotificationClientException(message: String) extends Exception(message)

trait NotificationsClient {
  val apiClient :ApiClient

  def send(payload: GoalAlertPayload): Future[Either[ApiClientError, Unit]] = apiClient.send(payload).map {
    case Left(e) if e.isInstanceOf[TotalApiError] => throw new NotificationClientException(e.description)
    case x => x
  }
}

trait GuardianNotificationsClient extends NotificationsClient {
  override val apiClient = ApiClient(
    host = GoalNotificationsConfig.guardianNotificationsHost,
    apiKey = GoalNotificationsConfig.guardianNotificationsApiKey,
    legacyHost = GoalNotificationsConfig.guardianNotificationsLegacyHost,
    legacyApiKey = GoalNotificationsConfig.guardianNotificationsLegacyApiKey,
    httpProvider = NotificationHttpProvider
  )

}
