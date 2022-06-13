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

import org.junit.Test;

import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JacksonCodecTest {

    @Test
    public void testException() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        String s = objectMapper.writeValueAsString(new RuntimeException("adfasdfasdf"));
        System.out.println(s);

        RuntimeException e = objectMapper.readValue(s, RuntimeException.class);
        e.printStackTrace();
    }

    @Test
    public void testClass() throws JsonProcessingException {

        Product p = new Product();
        p.setCode("002");
        p.setName("cpu");
        p.setGroup("x86");

        Call call = new Call(JacksonCodecTest.class.getName(), "", 1000, "hello", new Object[] { p },
                new Class[] { Product.class });

        ObjectMapper objectMapper = extracted();

        String s = objectMapper.writeValueAsString(call);
        System.out.println(s);

        Reply r = new Reply();
        r.setData(new FocusException("test", new IOException("sdfasdf")));
        s = objectMapper.writeValueAsString(r);
        System.out.println(s);

        r = objectMapper.readValue(s, Reply.class);
        System.out.println("error is " + r.getData());

    }

    private ObjectMapper extracted() {

        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping();

        // JSON configuration not to serialize null field
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        // JSON configuration not to throw exception on empty bean class
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // JSON configuration for compatibility
        objectMapper.enable(Feature.ALLOW_UNQUOTED_FIELD_NAMES);

        return objectMapper;

    }

}
