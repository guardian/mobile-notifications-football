package com.gu.mobile.notifications.football.actors

import akka.actor.{Props, ActorLogging, Actor}
import com.gu.mobile.notifications.football.lib.Pa._
import pa.{MatchDay, PaClient}
import org.joda.time.DateTime
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import com.gu.mobile.notifications.football.lib.Lists

object MatchDayObserverActor {
  sealed trait Message

  case object Refresh extends Message
  case class UpdatedMatchDays(matchDays: List[MatchDay]) extends Message
  case class GoalEvent(goal: Goal, matchDay: MatchDay) extends Message
  case object GetCurrentLiveMatches extends Message
  case class CurrentLiveMatches(matchDays: List[MatchDay]) extends Message

  val RefreshDelay = 15.seconds

  def props(paClient: PaClient) = Props(classOf[MatchDayObserverActor], paClient)
}

/** Actor that polls PA's match day endpoint, watching for goals.
  *
  * If the actor sees any goals, it sends an alert to its parent.
  *
  * @param apiClient PA football client
  */
class MatchDayObserverActor(apiClient: PaClient) extends Actor with ActorLogging {
  import MatchDayObserverActor._
  import context.dispatcher

  var previousMatchDays: Option[List[MatchDay]] = None

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
      val liveMatches = newMatchDays.filter(_.liveMatch)
      log.info(s"Got ${newMatchDays.length} matches of which ${liveMatches.length} are live")
      if (liveMatches.nonEmpty) {
        log.info(s"Currently live: ${liveMatches.map(_.summaryString).mkString(", ")}")
      }

      previousMatchDays foreach { oldMatchDays =>
        /** Calculate delta */
        for ((oldMatchDay, newMatchDay) <- Lists.pairs(oldMatchDays, newMatchDays)(_.id)) {
          delta(oldMatchDay, newMatchDay) foreach { goal =>
            log.info(s"${goal.scorerName} scored for ${goal.scoringTeam.name} at ${goal.minute} minute")
            context.parent ! GoalEvent(goal, newMatchDay)
          }
        }
      }

      previousMatchDays = Some(newMatchDays)
    }

    case GetCurrentLiveMatches => sender ! CurrentLiveMatches(previousMatchDays getOrElse Nil)
  }

  def scheduleRefresh(delay: FiniteDuration) = context.system.scheduler.scheduleOnce(delay, self, Refresh)
}
