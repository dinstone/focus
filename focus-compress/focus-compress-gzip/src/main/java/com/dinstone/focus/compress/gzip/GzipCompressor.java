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
package com.dinstone.focus.compress.gzip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.dinstone.focus.compress.Compressor;

public class GzipCompressor implements Compressor {

    public static final String COMPRESSOR_ID = "gzip";

    @Override
    public String compressorId() {
        return COMPRESSOR_ID;
    }

    @Override
    public byte[] encode(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(data);
        gzip.close();
        return out.toByteArray();
    }

    @Override
    public byte[] decode(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        GZIPInputStream ungzip = new GZIPInputStream(new ByteArrayInputStream(data));
        byte[] buffer = new byte[2048];
        int count = 0;
        while ((count = ungzip.read(buffer)) >= 0) {
            baos.write(buffer, 0, count);
        }
        ungzip.close();

        return baos.toByteArray();
    }

}
