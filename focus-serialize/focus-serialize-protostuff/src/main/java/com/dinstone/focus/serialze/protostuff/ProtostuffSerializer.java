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
package com.dinstone.focus.serialze.protostuff;

import java.io.IOException;

import com.dinstone.focus.serialize.Serializer;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.IdStrategy;
import io.protostuff.runtime.RuntimeSchema;

public class ProtostuffSerializer implements Serializer {

    public static final String SERIALIZER_TYPE = "protostuff";

    private static final DefaultIdStrategy STRATEGY = new DefaultIdStrategy(
            IdStrategy.DEFAULT_FLAGS | IdStrategy.PRESERVE_NULL_ELEMENTS | IdStrategy.MORPH_COLLECTION_INTERFACES
                    | IdStrategy.MORPH_MAP_INTERFACES | IdStrategy.MORPH_NON_FINAL_POJOS);

    @Override
    public String type() {
        return SERIALIZER_TYPE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] encode(Object content, Class<?> contentType) throws IOException {
        Schema<Object> schema = (Schema<Object>) getSchema(contentType);
        LinkedBuffer buffer = LinkedBuffer.allocate();
        try {
            return ProtostuffIOUtil.toByteArray(content, schema, buffer);
        } finally {
            buffer.clear();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object decode(byte[] contentBytes, Class<?> contentType) throws IOException {
        try {
            Schema<Object> schema = (Schema<Object>) getSchema(contentType);
            Object instance = contentType.newInstance();
            ProtostuffIOUtil.mergeFrom(contentBytes, instance, schema);
            return instance;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private Schema<?> getSchema(Class<?> cls) {
        return RuntimeSchema.getSchema(cls, STRATEGY);
    }

}
