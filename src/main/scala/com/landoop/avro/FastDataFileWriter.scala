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

import java.io._
import java.nio.ByteBuffer
import java.security.{MessageDigest, NoSuchAlgorithmException}
import java.util.UUID
import java.util.concurrent.Executors

import com.landoop.avro.FastDataFileWriter.NonCopyingByteArrayOutputStream
import com.landoop.avro.codec.CodecFactory
import com.landoop.avro.concurrent.FutureAwaitWithFailFastFn
import org.apache.avro.file.{Codec, DataFileConstants, Syncable}
import org.apache.avro.io.{BinaryEncoder, DatumWriter, EncoderFactory}
import org.apache.avro.{AvroRuntimeException, Schema}

import scala.concurrent.duration.{Duration, _}


/** Stores in a file a sequence of data conforming to a schema.  The schema is
  * stored in the file with the data.  Each datum in a file is of the same
  * schema.  Data is written with a {@link DatumWriter}.  Data is grouped into
  * <i>blocks</i>.  A synchronization marker is written between blocks, so that
  * files may be split.  Blocks may be compressed.  Extensible metadata is
  * stored at the end of the file.  Files may be appended to.
  *
  * @see DataFileReader
  */
class FastDataFileWriter[D] private[avro](datumWriter: DatumWriter[D],
                                          out: OutputStream,
                                          schema: Schema,
                                          codecFactory: CodecFactory,
                                          val flushOnEveryBlock: Boolean,
                                          val syncInterval: Int,
                                          syncMarker: Array[Byte],
                                          val parallelization: Int,
                                          meta: Map[String, Array[Byte]],
                                          encoderFactory: EncoderFactory) extends Closeable with Flushable {
  require(datumWriter != null, "Invalid DatumWriter")
  require(out != null, "Invalid output stream")
  require(schema != null, "Invalid schema")
  require(codecFactory != null, "Invalid codecFactory")
  require(syncMarker != null && syncMarker.length == 16, "Invalid syncMarker")
  require(parallelization > 0, "Invalid parallelization")
  require(syncInterval > 32 && syncInterval < (1 << 30), "Invalid syncInterval value: " + syncInterval)

  private val vout: BinaryEncoder = encoderFactory.binaryEncoder(out, null)
  private var isOpen = true
  private val lock = new Object
  private val codecs = (1 to parallelization).map(_ => codecFactory.createInstance()).toArray

  datumWriter.setSchema(schema)
  vout.writeFixed(DataFileConstants.MAGIC) // write magic
  vout.writeMapStart() // write metadata
  vout.setItemCount(meta.size)
  meta.foreach { case (key, value) =>
    vout.startItem()
    vout.writeString(key)
    vout.writeBytes(value)
  }
  vout.writeMapEnd()
  vout.writeFixed(syncMarker) // write initial sync
  vout.flush() //vout may be buffered, flush before writing to out


  private def assertOpen() = {
    if (!isOpen) throw new AvroRuntimeException("not open")
  }

  private def assertNotOpen() = {
    if (isOpen) throw new AvroRuntimeException("already open")
  }

  /** Write datum to the file.
    *
    * @see AppendWriteException
    */
  @throws[IOException]
  def write(data: IndexedSeq[D], threshold: Int = 50000, duration: Duration = 1.hour): Unit = {
    assertOpen()
    if (data.length < threshold) {
      implicit val codec = codecs(0)
      writeData(data, 0, data.length, false)
    } else {
      val threadPool = Executors.newFixedThreadPool(parallelization)
      val chunk = data.length / parallelization
      import com.landoop.avro.concurrent.ExecutorExtension._
      val futures = (0 until parallelization).map { i =>
        implicit val codec = codecs(i)
        if (i == parallelization - 1) {
          threadPool.submit {
            writeData(data, i * chunk, data.length, true)
          }
        } else {
          threadPool.submit {
            writeData(data, i * chunk, (i + 1) * chunk, true)
          }
        }
      }
      FutureAwaitWithFailFastFn(threadPool, futures, duration)
    }
  }

  private def writeData(data: IndexedSeq[D], from: Int, to: Int, synchronize: Boolean)(implicit codec: Codec) = {
    implicit val buffer = new FastDataFileWriter.NonCopyingByteArrayOutputStream(Math.min((syncInterval * 1.25).toInt, Integer.MAX_VALUE / 2 - 1))
    implicit val bufOut = encoderFactory.binaryEncoder(buffer, null)
    implicit val withSynchronization = synchronize
    var blockCount = 0
    for (i <- from until to) {
      datumWriter.write(data(i), bufOut)
      blockCount += 1
      if (writeIfBlockFull(blockCount, synchronize)) {
        blockCount = 0
      }
    }
    writeBlock(blockCount, synchronize)
  }

  private def bufferInUse(implicit
                          bufOut: BinaryEncoder,
                          buffer: NonCopyingByteArrayOutputStream) = buffer.size + bufOut.bytesBuffered

  private def writeIfBlockFull(blockCount: Int, synchronize: Boolean)
                              (implicit codec: Codec, bufOut: BinaryEncoder, buffer: NonCopyingByteArrayOutputStream) = {
    if (bufferInUse >= syncInterval)
      writeBlock(blockCount, synchronize)
    else
      false
  }

  @throws[IOException]
  private def writeBlock(blockCount: Int, synchronize: Boolean)
                        (implicit codec: Codec, bufOut: BinaryEncoder, buffer: NonCopyingByteArrayOutputStream) = {
    if (blockCount > 0) {
      bufOut.flush()
      val uncompressed = buffer.getByteArrayAsByteBuffer
      val block = new DataBlock(uncompressed, blockCount)
      block.setFlushOnWrite(flushOnEveryBlock)
      block.compressUsing(codec)
      if (synchronize) {
        lock.synchronized {
          block.writeBlockTo(vout, syncMarker)
        }
      } else {
        block.writeBlockTo(vout, syncMarker)
      }
      buffer.reset()
      true
    } else false
  }


  /**
    * Flushes the current state of the file.
    */
  override def flush(): Unit = {
    vout.flush()
  }

  /**
    * If this writer was instantiated using a File or using an
    * {@linkplain Syncable} instance, this method flushes all buffers for this
    * writer to disk. In other cases, this method behaves exactly
    * like {@linkplain #flush()}.
    *
    * @throws IOException
    */
  @throws[IOException]
  def fSync(): Unit = {
    flush()
    out match {
      case s: Syncable => s.sync()
      case _ =>
    }
  }

  /** Flush and close the file. */
  @throws[IOException]
  override def close(): Unit = {
    if (isOpen) {
      flush()
      out.close()
      isOpen = false
    }
  }
}

object FastDataFileWriter {
  private[avro] def generateSync = {
    try {
      val digester = MessageDigest.getInstance("MD5")
      val time = System.currentTimeMillis
      digester.update((UUID.randomUUID + "@" + time).getBytes)
      digester.digest
    } catch {
      case e: NoSuchAlgorithmException => throw new RuntimeException(e)
    }
  }

  def isReservedMeta(key: String): Boolean = key.startsWith("avro.")

  private class NonCopyingByteArrayOutputStream private[avro](val initialSize: Int) extends ByteArrayOutputStream(initialSize) {
    private[avro] def getByteArrayAsByteBuffer = ByteBuffer.wrap(buf, 0, count)
  }

}