package com.dinstone.focus.serializer.json;

import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.serializer.JacksonSerializer;

public class ReplySerializer extends JacksonSerializer<Reply> {

    @Override
    public String name() {
        return "json:call";
    }

    @Override
    public Object decode(byte[] bytes) throws Exception {
        return deserialize(bytes, Reply.class);
    }

    @Override
    public Object decode(byte[] bytes, int offset, int length) throws Exception {
        return deserialize(bytes, offset, length, Reply.class);
    }

}
