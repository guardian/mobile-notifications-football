package com.gu.mobile.notifications.football.actors

import akka.actor.ActorSystem

object TestActorSystem {
  implicit val testActorSystem = ActorSystem("test")
}
