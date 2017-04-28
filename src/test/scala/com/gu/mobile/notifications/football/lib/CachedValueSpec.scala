package com.gu.mobile.notifications.football.lib

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

import scala.concurrent.Future
import scala.concurrent.duration._
import org.joda.time.DateTime

class CachedValueSpec(implicit ev: ExecutionEnv) extends Specification {
  "A CachedValue" should {
    "Execute function to set value" in {
      val cv = new CachedValue[String](60.seconds)
      val result = cv() {
        Future.successful("test")
      }
      result must beEqualTo("test").await
    }

    "Do not execute function if existing value has not expired" in {
      val cv = new CachedValue[String](60.seconds)
      val result = cv() {
        Future.successful("first")
      } flatMap { _ =>
        Thread.sleep(100)
        cv() {
          Future.successful("second")
        }
      }
      result must beEqualTo("first").await
    }

    "Execute function second time if existing value has expired" in {
      val cv = new CachedValue[String](1.seconds)
      val result = cv() {
        Future.successful("first")
      } flatMap { _ =>
        Thread.sleep(2000)
        cv() {
          Future.successful("second")
        }
      }
      result must beEqualTo("second").await(0, 3.seconds)
    }
  }
}
