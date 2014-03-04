package com.gu.mobile.notifications.football.lib

trait ResourcesHelper {
  def slurp(path: String): Option[String] =
    Option(getClass.getClassLoader.getResource(path)).map(scala.io.Source.fromURL(_).mkString)

  def slurpOrDie(path: String): String =
    slurp(path).getOrElse(throw new RuntimeException(s"Required test fixture $path is not on your path"))
}