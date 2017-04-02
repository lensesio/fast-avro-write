package com.landoop.avro

import java.io.{BufferedOutputStream, File, FileOutputStream}
import java.util.UUID

import com.landoop.avro.codec.CodecFactory
import com.sksamuel.avro4s.{RecordFormat, SchemaFor}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.scalatest.{Matchers, WordSpec}

class FastDataFileWriterTest extends WordSpec with Matchers {
  "FastDataFileWriter" should {
    "write 50000 Stock Quotes" in {
      runTest(50000, 4)
    }

    "write 123341 Stock Quotes" in {
      runTest(123341, 4)
    }

    "write 1000000 Stock Quotes" in {
      runTest(1000000, 8)
    }

  }

  private def runTest(count: Int, parallelization: Int) = {
    val file = new File(UUID.randomUUID().toString + ".avro")
    file.deleteOnExit()
    try {

      val out = new BufferedOutputStream(new FileOutputStream(file), 4 * 1048576)
      val schema = SchemaFor[StockQuote]()
      val recordFormat = RecordFormat[StockQuote]
      val records = StockQuote.generate(count)
      import org.apache.avro.generic.GenericDatumWriter
      val datumWriter = new GenericDatumWriter[GenericRecord](schema)
      val builder = FastDataFileWriterBuilder(datumWriter, out, schema)
        .withCodec(CodecFactory.snappyCodec())
        .withFlushOnEveryBlock(false)
        .withParallelization(parallelization)

      builder.encoderFactory.configureBufferSize(4 * 1048576)
      builder.encoderFactory.configureBlockSize(4 * 1048576)

      val fileWriter = builder.build()
      fileWriter.write(records)
      fileWriter.close()

      import org.apache.avro.file.{DataFileConstants, DataFileReader}
      val datumReader = new GenericDatumReader[GenericRecord]()
      val reader = new DataFileReader[GenericRecord](file, datumReader)

      val scheamText = new String(reader.getMeta(DataFileConstants.SCHEMA))
      val actualSchema = new Schema.Parser().parse(scheamText)
      actualSchema shouldBe schema

      val codecMeta = reader.getMetaString(DataFileConstants.CODEC)
      codecMeta shouldBe CodecFactory.snappyCodec().createInstance().getName
      val iter = new Iterator[GenericRecord] {
        override def hasNext: Boolean = reader.hasNext

        override def next(): GenericRecord = reader.next()
      }
      val actualRecordsCount = iter.foldLeft(0) { case (total, r) =>
        val quote = recordFormat.from(r)
        quote.symbol shouldBe StockQuote.SampleQuote.symbol

        total + 1
      }

      actualRecordsCount shouldBe count
      reader.close()
    }
    finally {
      if (file.exists()) file.delete()
    }
  }
}
