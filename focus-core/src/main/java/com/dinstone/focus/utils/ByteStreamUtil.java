/*
 * Copyright (C) 2019~2020 dinstone<dinstone@163.com>
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
package com.dinstone.focus.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.netty.util.CharsetUtil;

public class ByteStreamUtil {

    public static int readInt(InputStream in) throws IOException {
        return ((byte) in.read() & 0xff) << 24 | ((byte) in.read() & 0xff) << 16 | ((byte) in.read() & 0xff) << 8
                | (byte) in.read() & 0xff;
    }

    public static void writeInt(OutputStream out, int i) throws IOException {
        out.write((byte) (i >> 24));
        out.write((byte) (i >> 16));
        out.write((byte) (i >> 8));
        out.write(i);
    }

    public static String readString(InputStream bai) throws IOException {
        int length = readInt(bai);
        if (length < 0) {
            return null;
        } else if (length == 0) {
            return "";
        } else {
            return new String(bai.readNBytes(length), CharsetUtil.UTF_8);
        }
    }

    public static void writeString(OutputStream bao, String str) throws IOException {
        if (str == null) {
            writeInt(bao, -1);
        } else if (str.isEmpty()) {
            writeInt(bao, 0);
        } else {
            byte[] strBytes = str.getBytes(CharsetUtil.UTF_8);
            writeInt(bao, strBytes.length);
            bao.write(strBytes);
        }
    }

}
