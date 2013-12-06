package com.gu.mobile.notifications.football.actors

import akka.actor.{Props, ActorLogging, Actor}
import com.gu.mobile.notifications.football.lib.Pa._
import pa.{MatchDay, PaClient}
import org.joda.time.DateTime
import scala.util.{Failure, Success}
import scala.concurrent.duration._

object MatchLifecycleActor {
  sealed trait Message

  case object Refresh extends Message
  case class UpdatedMatchDays(matchDays: List[MatchDay])

  val RefreshDelay = 15.seconds

  def props(paClient: PaClient) = Props(classOf[MatchLifecycleActor], paClient)
}

/** Actor that polls PA's match day endpoint, watching for goals.
  *
  * If the actor sees any goals, it sends an alert to the Event Bus.
  *
  * @param apiClient PA football client
  */
class MatchLifecycleActor(apiClient: PaClient) extends Actor with ActorLogging {
  import MatchLifecycleActor._
  import context.dispatcher

  var matchDays: List[MatchDay] = Nil

  self ! Refresh

  def receive = {
    case Refresh => {
      log.info("Refreshing ...")

      apiClient.matchDay(DateTime.now.toDateMidnight) onComplete {
        case Success(newMatchDays) => {
          self ! UpdatedMatchDays(newMatchDays)
          scheduleRefresh(RefreshDelay)
        }

        case Failure(error) => {
          log.error(s"Error refreshing match days: ${error.getMessage}")
          scheduleRefresh(RefreshDelay)
        }
      }
    }

    case UpdatedMatchDays(newMatchDays) => {
      newMatchDays foreach { matchDay =>
        log.info(s"Updated match info for ${matchDay.summaryString}")
      }


    }
  }

  def scheduleRefresh(delay: FiniteDuration) = context.system.scheduler.scheduleOnce(delay, self, Refresh)
}
