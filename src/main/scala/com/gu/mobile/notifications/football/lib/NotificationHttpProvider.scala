package com.gu.mobile.notifications.football.lib

import scala.concurrent.{ExecutionContext, Future}
import com.gu.mobile.notifications.client._
import com.ning.http.client.Response
import dispatch.{Http, as, url}

class NotificationHttpProvider(implicit ec: ExecutionContext) extends HttpProvider {

  override def post(uri: String, contentType: ContentType, body: Array[Byte]): Future[HttpResponse] =
    Http(
      url(uri)
        .POST
        .setContentType(contentType.mediaType, contentType.charset)
        .setBody(body) > as.Response(extract)
    )

  override def get(uri: String): Future[HttpResponse] =
    Http(url(uri) OK as.Response(extract))

  private def extract(response: Response) = {
    if (response.getStatusCode >= 200 && response.getStatusCode < 300)
      HttpOk(response.getStatusCode, response.getResponseBody)
    else
      HttpError(response.getStatusCode, response.getResponseBody)
  }
}