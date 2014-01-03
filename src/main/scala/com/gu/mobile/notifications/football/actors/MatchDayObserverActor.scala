package com.gu.mobile.notifications.football.actors

import akka.actor.{Props, ActorLogging, Actor}
import com.gu.mobile.notifications.football.lib.Pa._
import pa.MatchDay
import com.gu.mobile.notifications.football.lib.Lists

object MatchDayObserverActor {
  sealed trait Message

  case class UpdatedMatchDays(matchDays: List[MatchDay]) extends Message
  case class GoalEvent(goal: Goal, matchDay: MatchDay) extends Message
  case object GetCurrentLiveMatches extends Message
  case class CurrentLiveMatches(matchDays: List[MatchDay]) extends Message

  def props() = Props(classOf[MatchDayObserverActor])
}


/** Actor that on receiving match day update events, watches for goals (and emits them as events!)
  *
  * If the actor sees any goals, it sends an alert to its parent.
  */
class MatchDayObserverActor extends Actor with ActorLogging {
  import MatchDayObserverActor._

  var previousMatchDays: Option[List[MatchDay]] = None

  def receive = {
    case UpdatedMatchDays(newMatchDays) => {
      val liveMatches = newMatchDays.filter(_.liveMatch)
      log.info(s"Got ${newMatchDays.length} matches of which ${liveMatches.length} are live")
      if (liveMatches.nonEmpty) {
        log.info(s"Currently live: ${liveMatches.map(_.summaryString).mkString(", ")}")
      }

      val observer = sender

      previousMatchDays foreach { oldMatchDays =>
        /** Calculate delta */
        for ((oldMatchDay, newMatchDay) <- Lists.pairs(oldMatchDays, newMatchDays)(_.id)) {
          delta(oldMatchDay, newMatchDay) foreach { goal =>
            log.info(s"${goal.scorerName} scored for ${goal.scoringTeam.name} at ${goal.minute} minute")
            observer ! GoalEvent(goal, newMatchDay)
          }
        }
      }

      previousMatchDays = Some(newMatchDays)
    }

    case GetCurrentLiveMatches => sender ! CurrentLiveMatches(previousMatchDays getOrElse Nil)
  }
}
