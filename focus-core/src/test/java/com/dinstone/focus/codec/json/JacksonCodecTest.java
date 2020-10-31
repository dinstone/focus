package com.dinstone.focus.codec.json;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonCodecTest {

    @Test
    public void test() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        String s = objectMapper.writeValueAsString(new RuntimeException("adfasdfasdf"));
        System.out.println(s);

        RuntimeException e = objectMapper.readValue(s, RuntimeException.class);
        e.printStackTrace();

    }

}
