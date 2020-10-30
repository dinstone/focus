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
package com.dinstone.focus.codec.json;

import java.io.IOException;

import com.dinstone.focus.codec.CodecException;
import com.dinstone.focus.codec.RpcCodec;
import com.dinstone.focus.rpc.Attach;
import com.dinstone.focus.rpc.Call;
import com.dinstone.focus.rpc.Reply;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JacksonCodec implements RpcCodec {

    private ObjectMapper objectMapper;

    public JacksonCodec() {
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

    @Override
    public byte code() {
        return 1;
    }

    @Override
    public void encode(Request request, Call call) throws CodecException {
        try {
            request.setHeaders(Attach.encode(call.attach()));
            request.setContent(objectMapper.writeValueAsBytes(call));
        } catch (JsonProcessingException e) {
            throw new CodecException("encode call content error", e);
        } catch (IOException e) {
            throw new CodecException("encode call attachs error", e);
        }
    }

    @Override
    public Call decode(Request request) throws CodecException {
        try {
            Attach attach = Attach.decode(request.getHeaders());
            return objectMapper.readValue(request.getContent(), Call.class).attach(attach);
        } catch (IOException e) {
            throw new CodecException("decode call message error", e);
        }
    }

    @Override
    public void encode(Response response, Reply reply) throws CodecException {
        try {
            response.setHeaders(Attach.encode(reply.attach()));
            response.setContent(objectMapper.writeValueAsBytes(reply));
        } catch (JsonProcessingException e) {
            throw new CodecException("encode reply content error", e);
        } catch (IOException e) {
            throw new CodecException("encode reply attachs error", e);
        }
    }

    @Override
    public Reply decode(Response response) throws CodecException {
        try {
            Attach attach = Attach.decode(response.getHeaders());
            return objectMapper.readValue(response.getContent(), Reply.class).attach(attach);
        } catch (IOException e) {
            throw new CodecException("decode reply message error", e);
        }
    }

}
