package com.gu.mobile.notifications.football.lib

object Lists {
  /** Pairs up elements from two lists given a key function */
  def pairs[A, B](xs: List[A], ys: List[A])(f: A => B) = {
    val grouped1 = xs.groupBy(f)
    val grouped2 = ys.groupBy(f)

    for {
      key <- grouped1.keySet intersect grouped2.keySet
    } yield (grouped1(key).head, grouped2(key).head)
  }
}
