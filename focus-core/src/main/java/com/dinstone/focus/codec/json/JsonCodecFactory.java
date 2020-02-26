package com.dinstone.focus.codec.json;

import com.dinstone.focus.codec.Codec;
import com.dinstone.focus.codec.CodecFactory;

public class JsonCodecFactory implements CodecFactory {

    @Override
    public String getSchema() {
        return "json";
    }

    @Override
    public Codec createCodec() {
        return new JacksonCodec();
    }

}
