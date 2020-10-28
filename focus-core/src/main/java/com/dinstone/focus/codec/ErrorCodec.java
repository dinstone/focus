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
package com.dinstone.focus.codec;

import java.util.HashMap;
import java.util.Map;

import com.dinstone.focus.RpcException;
import com.dinstone.photon.message.Response;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ErrorCodec {

    private TypeReference<Map<String, String>> type = new TypeReference<Map<String, String>>() {
    };

    private ObjectMapper objectMapper;

    public ErrorCodec() {
        objectMapper = new ObjectMapper();
        // objectMapper.enableDefaultTyping();

        // JSON configuration not to serialize null field
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        // JSON configuration not to throw exception on empty bean class
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // JSON configuration for compatibility
        objectMapper.enable(Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        objectMapper.enable(Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
    }

    public void encode(Response response, RpcException exception) {
        Map<String, String> error = new HashMap<>();
        error.put("error.code", "" + exception.getCode());
        error.put("error.message", exception.getMessage());
        error.put("error.stack", exception.getStack());

        try {
            response.setContent(objectMapper.writeValueAsBytes(error));
        } catch (JsonProcessingException e) {
            // ignore
        }
    }

    public RpcException decode(Response response) {
        RpcException error = null;
        try {
            Map<String, String> headers = objectMapper.readValue(response.getContent(), type);
            int code = Integer.parseInt(headers.get("error.code"));
            String message = headers.get("error.message");
            String stack = headers.get("error.stack");
            error = new RpcException(code, message, stack);
        } catch (Exception e) {
            error = new RpcException(499, "decode exception error", e);
        }
        return error;
    }
}
