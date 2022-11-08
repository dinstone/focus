/*
 * Copyright (C) 2019~2022 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dinstone.focus.compress.lz4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.dinstone.focus.compress.Compressor;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

public class Lz4Compressor implements Compressor {

    public static final String COMPRESSOR_TYPE = "lz4";

    private LZ4FastDecompressor decompressor;
    private LZ4Compressor compressor;

    public Lz4Compressor() {
        LZ4Factory factory = LZ4Factory.fastestInstance();
        decompressor = factory.fastDecompressor();
        compressor = factory.fastCompressor();
    }

    @Override
    public String compressorType() {
        return COMPRESSOR_TYPE;
    }

    @Override
    public byte[] encode(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        LZ4BlockOutputStream compressedOutput = new LZ4BlockOutputStream(baos, 2048, compressor);
        compressedOutput.write(data);
        compressedOutput.close();

        return baos.toByteArray();
    }

    @Override
    public byte[] decode(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        LZ4BlockInputStream lzis = new LZ4BlockInputStream(new ByteArrayInputStream(data), decompressor);
        int count = 0;
        byte[] buffer = new byte[2048];
        while ((count = lzis.read(buffer)) != -1) {
            baos.write(buffer, 0, count);
        }
        lzis.close();

        return baos.toByteArray();
    }

}
