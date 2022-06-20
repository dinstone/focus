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
package com.dinstone.focus.codec.json;

import java.io.IOException;

import com.dinstone.focus.codec.AbstractCodec;
import com.dinstone.focus.codec.CodecException;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JacksonCodec extends AbstractCodec {

    private ObjectMapper objectMapper;

    public JacksonCodec() {
        objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator());

        // JSON configuration not to serialize null field
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        // JSON configuration not to throw exception on empty bean class
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // JSON configuration for compatibility
        objectMapper.enable(Feature.ALLOW_UNQUOTED_FIELD_NAMES);
    }

    @Override
    public String codecId() {
        return "json/jackson";
    }

    @Override
    protected byte[] write(Object parameter, Class<?> paramType) {
        if (parameter == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsBytes(parameter);
        } catch (JsonProcessingException e) {
            throw new CodecException("encode call message error", e);
        }
    }

    @Override
    protected Object read(byte[] paramBytes, Class<?> paramType) {
        if (paramBytes == null || paramType == null) {
            return null;
        }
        try {
            return objectMapper.readValue(paramBytes, paramType);
        } catch (IOException e) {
            throw new CodecException("decode reply message error", e);
        }
    }

}
