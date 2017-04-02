package com.landoop.avro

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration._

trait Timed extends StrictLogging {
  def withTime[T](message: String)(thunk: => T): T = {
    val start = System.nanoTime()
    val r = thunk
    val end = System.nanoTime()
    val duration = Duration.create(end - start, TimeUnit.NANOSECONDS).toMillis
    logger.info(s"$message took $duration ms")
    r
  }

  def withTime(thunk: => Unit): Duration = {
    val start = System.nanoTime()
    val r = thunk
    val end = System.nanoTime()
    Duration.create(end - start, TimeUnit.NANOSECONDS)
  }
}
