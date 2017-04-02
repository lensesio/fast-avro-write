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
package com.landoop.avro;

import org.apache.avro.file.Codec;
import org.apache.avro.io.BinaryEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;

class DataBlock {
    private byte[] data;
    private long numEntries;
    private int blockSize;
    private int offset = 0;
    private boolean flushOnWrite = true;

    private DataBlock(long numEntries, int blockSize) {
        this.data = new byte[blockSize];
        this.numEntries = numEntries;
        this.blockSize = blockSize;
    }

    DataBlock(ByteBuffer block, long numEntries) {
        this.data = block.array();
        this.blockSize = block.remaining();
        this.offset = block.arrayOffset() + block.position();
        this.numEntries = numEntries;
    }

    byte[] getData() {
        return data;
    }

    long getNumEntries() {
        return numEntries;
    }

    int getBlockSize() {
        return blockSize;
    }

    boolean isFlushOnWrite() {
        return flushOnWrite;
    }

    void setFlushOnWrite(boolean flushOnWrite) {
        this.flushOnWrite = flushOnWrite;
    }

    ByteBuffer getAsByteBuffer() {
        return ByteBuffer.wrap(data, offset, blockSize);
    }

    void decompressUsing(Codec c) throws IOException {
        ByteBuffer result = c.decompress(getAsByteBuffer());
        data = result.array();
        blockSize = result.remaining();
    }

    void compressUsing(Codec c) throws IOException {
        ByteBuffer result = c.compress(getAsByteBuffer());
        data = result.array();
        blockSize = result.remaining();
    }

    void writeBlockTo(BinaryEncoder e, byte[] sync) throws IOException {
        e.writeLong(this.numEntries);
        e.writeLong(this.blockSize);
        e.writeFixed(this.data, offset, this.blockSize);
        e.writeFixed(sync);
        if (flushOnWrite) {
            e.flush();
        }
    }

}