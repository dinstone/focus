/*
 * Copyright (C) 2019~2021 dinstone<dinstone@163.com>
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
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
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
    public byte codecId() {
        return 1;
    }

    @Override
    protected byte[] writeCall(Call call) throws CodecException {
        try {
            return objectMapper.writeValueAsBytes(call);
        } catch (JsonProcessingException e) {
            throw new CodecException("encode call message error", e);
        }
    }

    @Override
    protected Call readCall(byte[] content) {
        try {
            return objectMapper.readValue(content, Call.class);
        } catch (IOException e) {
            throw new CodecException("decode call message error", e);
        }
    }

    @Override
    protected byte[] writeReply(Reply reply) {
        try {
            return objectMapper.writeValueAsBytes(reply);
        } catch (JsonProcessingException e) {
            throw new CodecException("encode reply message error", e);
        }
    }

    @Override
    protected Reply readReply(byte[] content) {
        try {
            return objectMapper.readValue(content, Reply.class);
        } catch (IOException e) {
            throw new CodecException("decode reply message error", e);
        }
    }

}
