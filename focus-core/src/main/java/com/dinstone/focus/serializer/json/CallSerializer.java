package com.dinstone.focus.serializer.json;

import com.dinstone.focus.rpc.Call;

public class CallSerializer extends JacksonSerializer<Call> {

    @Override
    public String name() {
        return "json:call";
    }

    @Override
    public Call decode(byte[] bytes) throws Exception {
        return deserialize(bytes, Call.class);
    }

    @Override
    public Call decode(byte[] bytes, int offset, int length) throws Exception {
        return deserialize(bytes, offset, length, Call.class);
    }

}
