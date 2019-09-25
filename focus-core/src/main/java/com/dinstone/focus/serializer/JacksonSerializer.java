/*
 * Copyright (C) 2013~2017 dinstone<dinstone@163.com>
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
package com.dinstone.focus.serializer;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public abstract class JacksonSerializer<T> implements Serializer {

    private ObjectMapper objectMapper;

    public JacksonSerializer() {
        objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping();

        // JSON configuration not to serialize null field
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        // JSON configuration not to throw exception on empty bean class
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // JSON configuration for compatibility
        objectMapper.enable(Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        objectMapper.enable(Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
    }

    protected T deserialize(byte[] bytes, Class<T> clazz) throws Exception {
        return objectMapper.readValue(bytes, clazz);
    }

    protected T deserialize(byte[] bytes, int offset, int length, Class<T> clazz) throws Exception {
        return objectMapper.readValue(bytes, offset, length, clazz);
    }

    @Override
    public byte[] encode(Object data) throws Exception {
        return objectMapper.writeValueAsBytes(data);
    }
}
