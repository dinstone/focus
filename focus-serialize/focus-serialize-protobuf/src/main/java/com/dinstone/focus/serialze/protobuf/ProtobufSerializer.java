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
package com.dinstone.focus.serialze.protobuf;

import java.io.IOException;
import java.lang.reflect.Method;

import com.dinstone.focus.serialize.Serializer;
import com.google.protobuf.MessageLite;

public class ProtobufSerializer implements Serializer {

    private static final String PARSE_FROM_METHOD = "parseFrom";

    @Override
    public String serializerId() {
        return "protobuf";
    }

    @Override
    public byte[] encode(Object content, Class<?> contentType) throws IOException {
        if (content instanceof MessageLite) {
            return ((MessageLite) content).toByteArray();
        }
        throw new IOException("unsported parameter type");
    }

    @Override
    public Object decode(byte[] contentBytes, Class<?> contentType) throws IOException {
        if (contentBytes == null) {
            return null;
        }
        try {
            Method m = contentType.getMethod(PARSE_FROM_METHOD, byte[].class);
            return m.invoke(null, contentBytes);
        } catch (Exception e) {
            throw new IOException("read parameter error", e);
        }
    }

}
