package com.landoop.avro

import java.io.{BufferedOutputStream, File, FileOutputStream}

import com.landoop.avro.codec.CodecFactory
import org.apache.avro.Schema
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.GenericRecord

object AvroFileWriter {
  def fastWrite(file: File,
                count: Int,
                parallelization: Int,
                schema: Schema,
                records: IndexedSeq[GenericRecord]) = {
    val out = new BufferedOutputStream(new FileOutputStream(file), 4 * 1048576)

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
  }

  def write(file: File,
            count: Int,
            schema: Schema,
            records: Seq[GenericRecord]) = {
    val out = new BufferedOutputStream(new FileOutputStream(file), 4 * 1048576)
    
    import org.apache.avro.generic.GenericDatumWriter
    val datumWriter = new GenericDatumWriter[GenericRecord](schema)
    val writer = new DataFileWriter(datumWriter)
      .setCodec(org.apache.avro.file.CodecFactory.snappyCodec())
      .create(schema, out)

    writer.setFlushOnEveryBlock(false)

    records.foreach(writer.append)
    writer.close()
  }
}
