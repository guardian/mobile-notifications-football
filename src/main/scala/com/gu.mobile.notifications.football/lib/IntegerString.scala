package com.gu.mobile.notifications.football.lib

import scala.util.Try

object IntegerString {
  def unapply(str: String): Option[Int] = Try {
    Integer.parseInt(str)
  }.toOption
}
