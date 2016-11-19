package com.gu.mobile.notifications.football

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.gu.mobile.notifications.football.lib.{ExpiringTopics, GoalNotificationBuilder, PaExpiringTopics, PaFootballClient}
import ExpirationJsonImplicits._
import com.gu.mobile.notifications.football.conf.GoalNotificationsConfig
import spray.routing.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import spray.routing.authentication.ContextAuthenticator

import scala.concurrent.Future

class GoalNotificationsServiceActor extends Actor with GoalNotificationsService {
  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)

  override val expiringTopics: ExpiringTopics = PaExpiringTopics(PaFootballClient)
}

// this trait defines our service behavior independently from the service actor
trait GoalNotificationsService extends HttpService {
  implicit val timeout = Timeout(500 millis)

  val authenticator: ContextAuthenticator[Unit] = { ctx =>
    Future {
      val maybeKey = ctx.request.headers.find(_.name == "api-key").map(_.value)
      maybeKey match {
        case None => Left(AuthenticationFailedRejection(CredentialsMissing, List()))
        case GoalNotificationsConfig.goalAlertsApiKey => Right()
        case Some(_) => Left(AuthenticationFailedRejection(CredentialsRejected, List()))
      }
    }
  }
  val expiringTopics: ExpiringTopics

  val commonEndPoints =
    path("expired-topics") {
      post {
        decompressRequest() {
          entity(as[ExpirationRequest]) { request =>
            detach() {
              complete {
                expiringTopics.getExpired(request.topics) map { expiredTopics =>
                  ExpirationResponse(expiredTopics)
                }
              }
            }
          }
        }
      }
    } ~
      path("healthcheck") {
        get {
          if (PAHealthCheck.contactedPaSuccessfully)
            complete(StatusCodes.OK, "healthy")
          else
            complete(StatusCodes.InternalServerError)
        }
      }
  val testEndpoint =
    path("send-test-notification") {
      post {
        decompressRequest() {
          authenticate(authenticator) { _ =>
            entity(as[GoalAndMetadata]) { case GoalAndMetadata(goal, metadata) =>
              detach() {
                respondWithMediaType(`text/html`) {
                  complete {
                    GoalNotificationsPipeline.send(GoalNotificationBuilder(goal, metadata)) map { _ =>
                      <p>Sent a test notification</p>
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

  val myRoute = if (GoalNotificationsConfig.testEndPointEnabled) commonEndPoints ~ testEndpoint else commonEndPoints
}
