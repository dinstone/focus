/*
 * Copyright (C) 2019~2023 dinstone<dinstone@163.com>
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
package com.dinstone.focus.compress;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.dinstone.focus.compress.gzip.GzipCompressor;
import com.dinstone.focus.compress.lz4.Lz4Compressor;
import com.dinstone.focus.compress.snappy.SnappyCompressor;

public class CompressTest {

    public static final String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String generateString(int length) {
        StringBuffer sb = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(allChar.charAt(random.nextInt(allChar.length())));
        }
        return sb.toString();
    }

    public static String generateString1(int length) {
        Map<String, String> p = new HashMap<String, String>();
        p.put("sn", "S001");
        p.put("uid", "U981");
        p.put("poi", "20910910");
        p.put("ct", "2022-06-17");

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            sb.append(p.toString());
            if (sb.length() >= length) {
                break;
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {

        String data = generateString1(1024 * 10);

        Compressor snac = CompressorFactory.lookup(SnappyCompressor.COMPRESSOR_TYPE);
        compress(snac, data);

        Compressor lz4c = CompressorFactory.lookup(Lz4Compressor.COMPRESSOR_TYPE);
        compress(lz4c, data);

        Compressor gzip = CompressorFactory.lookup(GzipCompressor.COMPRESSOR_TYPE);
        compress(gzip, data);
    }

    private static void compress(Compressor compress, String data) throws IOException {
        byte[] dataBytes = data.getBytes();
        System.out.println("encode before " + dataBytes.length);

        byte[] resultBytes = compress.encode(dataBytes);
        System.out.println("encode after  " + resultBytes.length);

        byte[] uncompressBytes = compress.decode(resultBytes);
        System.out.println("decode after  " + uncompressBytes.length);
        String result = new String(uncompressBytes);
        System.out.println(compress.compressorType() + " : " + result.equals(data));
        System.out.println("===============================");
    }

}
