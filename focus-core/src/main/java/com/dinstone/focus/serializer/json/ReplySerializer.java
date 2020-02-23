package com.dinstone.focus.serializer.json;

import com.dinstone.focus.rpc.Reply;

public class ReplySerializer extends JacksonSerializer<Reply> {

    @Override
    public String name() {
        return "json:reply";
    }

    @Override
    public Reply decode(byte[] bytes) throws Exception {
        return deserialize(bytes, Reply.class);
    }

    @Override
    public Reply decode(byte[] bytes, int offset, int length) throws Exception {
        return deserialize(bytes, offset, length, Reply.class);
    }

}
