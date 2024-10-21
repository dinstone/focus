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
package com.dinstone.focus.serialize.json;

import java.io.IOException;

import com.dinstone.focus.serialize.Serializer;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JacksonSerializer implements Serializer {

    public static final String SERIALIZER_TYPE = "json";

    private final ObjectMapper objectMapper;

    public JacksonSerializer() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator());

        // JSON configuration not to serialize null field
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        // JSON configuration not to throw exception on empty bean class
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // JSON configuration for compatibility
        objectMapper.enable(Feature.ALLOW_UNQUOTED_FIELD_NAMES);
    }

    @Override
    public String type() {
        return SERIALIZER_TYPE;
    }

    @Override
    public byte[] encode(Object content, Class<?> contentType) throws IOException {
        if (content == null) {
            return null;
        }
        return objectMapper.writeValueAsBytes(content);
    }

    @Override
    public Object decode(byte[] contentBytes, Class<?> contentType) throws IOException {
        if (contentBytes == null || contentType == null) {
            return null;
        }
        return objectMapper.readValue(contentBytes, contentType);
    }

}
