package com.dinstone.focus.serializer.json;

import java.util.HashMap;
import java.util.Map;

import com.dinstone.focus.RpcException;

public class ExceptionSerializer extends JacksonSerializer<RpcException> {

    @Override
    public String name() {
        return "json:exception";
    }

    @Override
    public byte[] encode(RpcException data) throws Exception {
        Map<String, String> em = new HashMap<>();
        em.put("code", "" + data.getCode());
        em.put("message", data.getMessage());
        return objectMapper.writeValueAsBytes(data);
    }

    @Override
    public RpcException decode(byte[] bytes) throws Exception {
        return decode(bytes, 0, bytes.length);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RpcException decode(byte[] bytes, int offset, int length) throws Exception {
        Map<String, String> em = objectMapper.readValue(bytes, Map.class);
        return new RpcException(Integer.parseInt(em.get("code")), em.get("message"));
    }

}
