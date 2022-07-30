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
package com.dinstone.focus.codec.protobuf;

import java.lang.reflect.Method;

import com.dinstone.focus.codec.AbstractCodec;
import com.dinstone.focus.codec.CodecException;
import com.google.protobuf.MessageLite;

public class ProtobufCodec extends AbstractCodec {

    public static final String CODEC_ID = "boen/protobuf";

    @Override
    public String codecId() {
        return CODEC_ID;
    }

    @Override
    protected Object read(byte[] paramBytes, Class<?> paramType) {
        if (paramBytes == null) {
            return null;
        }
        try {
            Method m = paramType.getMethod("parseFrom", byte[].class);
            return m.invoke(null, paramBytes);
        } catch (Exception e) {
            throw new CodecException("read parameter error", e);
        }
    }

    @Override
    protected byte[] write(Object parameter, Class<?> paramType) {
        if (parameter instanceof MessageLite) {
            return ((MessageLite) parameter).toByteArray();
        }
        throw new CodecException("unsported parameter type");
    }

}
