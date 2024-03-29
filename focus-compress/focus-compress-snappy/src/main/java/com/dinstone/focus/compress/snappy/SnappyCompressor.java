/*
 * Copyright (C) 2019~2024 dinstone<dinstone@163.com>
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
package com.dinstone.focus.compress.snappy;

import java.io.IOException;

import org.xerial.snappy.Snappy;

import com.dinstone.focus.compress.Compressor;

public class SnappyCompressor implements Compressor {

    public static final String COMPRESSOR_TYPE = "snappy";

    @Override
    public byte[] encode(byte[] data) throws IOException {
        return Snappy.compress(data);
    }

    @Override
    public byte[] decode(byte[] data) throws IOException {
        return Snappy.uncompress(data);
    }

    @Override
    public String type() {
        return COMPRESSOR_TYPE;
    }

}