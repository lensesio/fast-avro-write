package com.landoop.avro.codec;

import org.apache.avro.file.Codec;
import org.apache.avro.file.DataFileConstants;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * Implements xz compression and decompression.
 */
public class XZCodec extends Codec {


    private ByteArrayOutputStream outputBuffer;
    private int compressionLevel;

    public XZCodec(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    @Override
    public String getName() {
        return DataFileConstants.XZ_CODEC;
    }

    @Override
    public ByteBuffer compress(ByteBuffer data) throws IOException {
        ByteArrayOutputStream baos = getOutputBuffer(data.remaining());
        OutputStream ios = new XZCompressorOutputStream(baos, compressionLevel);
        writeAndClose(data, ios);
        return ByteBuffer.wrap(baos.toByteArray());
    }

    @Override
    public ByteBuffer decompress(ByteBuffer data) throws IOException {
        ByteArrayOutputStream baos = getOutputBuffer(data.remaining());
        InputStream bytesIn = new ByteArrayInputStream(
                data.array(),
                data.arrayOffset() + data.position(),
                data.remaining());
        InputStream ios = new XZCompressorInputStream(bytesIn);
        try {
            IOUtils.copy(ios, baos);
        } finally {
            ios.close();
        }
        return ByteBuffer.wrap(baos.toByteArray());
    }

    private void writeAndClose(ByteBuffer data, OutputStream to) throws IOException {
        byte[] input = data.array();
        int offset = data.arrayOffset() + data.position();
        int length = data.remaining();
        try {
            to.write(input, offset, length);
        } finally {
            to.close();
        }
    }

    // get and initialize the output buffer for use.
    private ByteArrayOutputStream getOutputBuffer(int suggestedLength) {
        if (null == outputBuffer) {
            outputBuffer = new ByteArrayOutputStream(suggestedLength);
        }
        outputBuffer.reset();
        return outputBuffer;
    }

    @Override
    public int hashCode() {
        return compressionLevel;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || obj.getClass() != getClass())
            return false;
        XZCodec other = (XZCodec) obj;
        return (this.compressionLevel == other.compressionLevel);
    }

    @Override
    public String toString() {
        return getName() + "-" + compressionLevel;
    }
}
