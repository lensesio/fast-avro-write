package com.landoop.avro

import java.io.File
import java.util.UUID

import com.sksamuel.avro4s.{RecordFormat, SchemaFor}

object StandardWriteProgram extends App with Timed {

  val recordsCount = 1000000
  val schema = SchemaFor[StockQuote]()
  val recordFormat = RecordFormat[StockQuote]
  val records = StockQuote.generate(recordsCount)

  val runs = 10
  val files = (1 to runs + 1).map(_ => new File(UUID.randomUUID().toString + ".avro"))
    .toVector

  files.foreach(_.deleteOnExit())
  AvroFileWriter.write(files.last, recordsCount, schema, records)
  val stats = (1 to runs).map(files).map { f =>
    withTime {
      AvroFileWriter.write(f, recordsCount, schema, records)
    }
  }.toVector

  stats.zipWithIndex.foreach { case (d, i) =>
    logger.info(s"Run number $i took ${d.toMillis} ms")
    println(s"Run number $i took ${d.toMillis} ms")
  }

  logger.info(s"Min run took ${stats.min.toMillis} ms")
  println(s"Min run took ${stats.min.toMillis} ms")
  logger.info(s"Max run took ${stats.max.toMillis} ms")
  println(s"Max run took ${stats.max.toMillis} ms")
  logger.info(s"Avg run took ${stats.map(_.toMillis).sum / runs} ms")
  println(s"Avg run took ${stats.map(_.toMillis).sum / runs} ms")
}
