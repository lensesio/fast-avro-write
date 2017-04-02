/*
 *  Copyright 2017 Landoop.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.landoop.avro

import java.io.OutputStream

import com.landoop.avro.codec.CodecFactory
import org.apache.avro.file.DataFileConstants
import org.apache.avro.io.{DatumWriter, EncoderFactory}
import org.apache.avro.{AvroRuntimeException, Schema}

/**
  * Created by stefan on 01/04/2017.
  */
case class FastDataFileWriterBuilder[D] private(datumout: DatumWriter[D],
                                                outputStream: OutputStream,
                                                schema: Schema,
                                                codecFactory: CodecFactory,
                                                flushOnEveryBlock: Boolean = true,
                                                syncInterval: Int = DataFileConstants.DEFAULT_SYNC_INTERVAL,
                                                sync: Array[Byte] = null,
                                                parallelization: Int = 4,
                                                metaMap: Map[String, Array[Byte]] = Map.empty[String, Array[Byte]],
                                                encoderFactory: EncoderFactory = new EncoderFactory) {
  require(datumout != null, "Invalid DatumWriter")
  require(outputStream != null, "Invalid output stream")
  require(schema != null, "Invalid schema")


  def this(datumWriter: DatumWriter[D],
           outputStream: OutputStream,
           schema: Schema) = {
    this(datumWriter, outputStream, schema, CodecFactory.nullCodec())
  }

  /**
    * Creates a new instance of {@link FastDataFileWriter}
    *
    * @return
    */
  def build(): FastDataFileWriter[D] = {
    val syncMarker = {
      if (sync == null) FastDataFileWriter.generateSync
      else sync
    }

    new FastDataFileWriter[D](
      datumout,
      outputStream,
      schema,
      codecFactory,
      flushOnEveryBlock,
      syncInterval,
      syncMarker,
      parallelization,
      metaMap +
        (DataFileConstants.CODEC -> codecFactory.createInstance().getName.getBytes("UTF-8")) +
        (DataFileConstants.SCHEMA -> schema.toString().getBytes("UTF-8")),
      encoderFactory)
  }

  /**
    * Sets the codec to be used
    *
    * @param codecFactory - An instance of a codec factory
    * @return
    */
  def withCodec(codecFactory: CodecFactory): FastDataFileWriterBuilder[D] = {
    require(codecFactory != null, "Invalid codecFactory")
    copy(codecFactory = codecFactory, metaMap = metaMap + (DataFileConstants.CODEC -> codecFactory.createInstance().getName.getBytes("UTF-8")))
  }

  /**
    * Set whether this writer should flush the block to the stream every time
    * a sync marker is written. By default, the writer will flush the buffer
    * each time a sync marker is written (if the block size limit is reached
    * or the {@link FastDataFileWriter#sync()} is called.
    *
    * @param flag - If set to false, this writer will not flush
    *             the block to the stream until { @linkplain
    *             #flush()} is explicitly called.
    */
  def withFlushOnEveryBlock(flag: Boolean): FastDataFileWriterBuilder[D] = copy(flushOnEveryBlock = flag)

  /**
    * Adds metadata property
    *
    * @param key
    * @param value
    * @return the new instance of the builder
    */
  def withMeta(key: String, value: Array[Byte]): FastDataFileWriterBuilder[D] = {
    if (FastDataFileWriter.isReservedMeta(key)) throw new AvroRuntimeException("Cannot set reserved meta key: " + key)
    require(key != null && key.trim.nonEmpty, "Invalid key")
    require(value != null && value.nonEmpty, "Invalid value")
    copy(metaMap = metaMap + (key -> value))
  }


  /**
    * Adds metadata property
    *
    * @param key
    * @param value
    * @return the new instance of the builder
    */
  def withMeta(key: String, value: String): FastDataFileWriterBuilder[D] = {
    require(value != null && value.trim.nonEmpty, "Invalid value")
    withMeta(key, value.getBytes("UTF-8"))
  }


  /**
    * Adds metadata property
    *
    * @param key
    * @param value
    * @return the new instance of the builder
    */
  def withMeta(key: String, value: Long): FastDataFileWriterBuilder[D] = withMeta(key, java.lang.Long.toString(value))

  /**
    * Set the synchronization interval for this file, in bytes.
    * Valid values range from 32 to `2^30`
    * Suggested values are between 2K and 2M
    *
    * The stream is flushed by default at the end of each synchronization
    * interval.
    *
    * If {@linkplain #setFlushOnEveryBlock(boolean)} is
    * called with param set to false, then the block may not be flushed to the
    * stream after the sync marker is written. In this case,
    * the {@linkplain #flush()} must be called to flush the stream.
    *
    * Invalid values throw IllegalArgumentException
    *
    * @param syncInterval
    * the approximate number of uncompressed bytes to write in each block
    * @return
    * this DataFileWriter
    */
  def withSyncInterval(syncInterval: Int): FastDataFileWriterBuilder[D] = {
    require(syncInterval > 32 && syncInterval < (1 << 30), "Invalid syncInterval value: " + syncInterval)
    copy(syncInterval = syncInterval)
  }

  /**
    * How many worker threads will be used to serialize to Avro.
    *
    * @param parallelization - Number of threads to run when serializing to avro
    * @return
    */
  def withParallelization(parallelization: Int): FastDataFileWriterBuilder[D] = {
    require(parallelization > 1, "Invalid parallelization")
    copy(parallelization = parallelization)
  }

  def withSync(sync: Array[Byte]): FastDataFileWriterBuilder[D] = {
    require(sync != null && sync.length == 16, "Invalid sync")
    copy(sync = sync)
  }
}


object FastDataFileWriterBuilder {
  def apply[D](datumWriter: DatumWriter[D], outputStream: OutputStream, schema: Schema) = {
    new FastDataFileWriterBuilder[D](datumWriter, outputStream, schema)
  }
}