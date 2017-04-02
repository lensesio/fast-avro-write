package com.landoop.avro.codec;

import org.apache.avro.file.Codec;
import org.apache.avro.file.DataFileConstants;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Implements bzip2 compression and decompression.
 */
public class BZip2Codec extends Codec {

    public static final int DEFAULT_BUFFER_SIZE = 64 * 1024;
    private ByteArrayOutputStream outputBuffer;

    @Override
    public String getName() {
        return DataFileConstants.BZIP2_CODEC;
    }

    @Override
    public ByteBuffer compress(ByteBuffer uncompressedData) throws IOException {

        ByteArrayOutputStream baos = getOutputBuffer(uncompressedData.remaining());
        BZip2CompressorOutputStream outputStream = new BZip2CompressorOutputStream(baos);

        try {
            outputStream.write(uncompressedData.array(),
                    uncompressedData.position(),
                    uncompressedData.remaining());
        } finally {
            outputStream.close();
        }

        ByteBuffer result = ByteBuffer.wrap(baos.toByteArray());
        return result;
    }

    @Override
    public ByteBuffer decompress(ByteBuffer compressedData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData.array());
        BZip2CompressorInputStream inputStream = new BZip2CompressorInputStream(bais);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

            int readCount = -1;

            while ((readCount = inputStream.read(buffer, compressedData.position(), buffer.length)) > 0) {
                baos.write(buffer, 0, readCount);
            }

            ByteBuffer result = ByteBuffer.wrap(baos.toByteArray());
            return result;
        } finally {
            inputStream.close();
        }
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || obj.getClass() != getClass())
            return false;
        return true;
    }

    //get and initialize the output buffer for use.
    private ByteArrayOutputStream getOutputBuffer(int suggestedLength) {
        if (null == outputBuffer) {
            outputBuffer = new ByteArrayOutputStream(suggestedLength);
        }
        outputBuffer.reset();
        return outputBuffer;
    }


}