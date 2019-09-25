package com.dinstone.focus.serializer.json;

import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.serializer.JacksonSerializer;

public class CallSerializer extends JacksonSerializer<Call> {

    @Override
    public String name() {
        return "json:call";
    }

    @Override
    public Object decode(byte[] bytes) throws Exception {
        return deserialize(bytes, Call.class);
    }

    @Override
    public Object decode(byte[] bytes, int offset, int length) throws Exception {
        return deserialize(bytes, offset, length, Call.class);
    }

}
