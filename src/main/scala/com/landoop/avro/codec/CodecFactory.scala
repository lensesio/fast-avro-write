package com.landoop.avro.codec

import org.apache.avro.file.Codec

sealed trait CodecFactory {
  def createInstance(): Codec
}

object CodecFactory {
  def nullCodec() = new CodecFactory {
    override def createInstance(): Codec = NullCodec.INSTANCE
  }

  def deflateCodec(compressionLevel: Int) = new CodecFactory {
    override def createInstance(): Codec = new DeflateCodec(compressionLevel)
  }

  def xzCodec(compressionLevel: Int) = new CodecFactory {
    override def createInstance(): Codec = new XZCodec(compressionLevel)
  }

  def bzip2Codec() = new CodecFactory {
    override def createInstance(): Codec = new BZip2Codec()
  }

  def snappyCodec() = new CodecFactory {
    override def createInstance(): Codec = new SnappyCodec()
  }
}
