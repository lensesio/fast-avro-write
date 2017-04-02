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
package com.landoop.avro.codec;

import org.apache.avro.file.Codec;
import org.apache.avro.file.DataFileConstants;

import java.io.IOException;
import java.nio.ByteBuffer;

/** Implements "null" (pass through) codec. */
final class NullCodec extends Codec {

    public static final NullCodec INSTANCE = new NullCodec();

    private NullCodec(){

    }

    @Override
    public String getName() {
        return DataFileConstants.NULL_CODEC;
    }

    @Override
    public ByteBuffer compress(ByteBuffer buffer) throws IOException {
        return buffer;
    }

    @Override
    public ByteBuffer decompress(ByteBuffer data) throws IOException {
        return data;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other != null && other.getClass() == getClass());
    }

    @Override
    public int hashCode() {
        return 2;
    }
}