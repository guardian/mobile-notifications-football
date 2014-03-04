package com.gu.mobile.notifications.football.lib

object Functions {
  def complement[A](f: A => Boolean): A => Boolean = { a: A => !f(a) }
}
