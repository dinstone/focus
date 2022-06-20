package com.dinstone.focus.codec.protobuf;

import com.dinstone.focus.codec.CodecFactory;
import com.dinstone.focus.codec.ProtocolCodec;

public class ProtobufFactory implements CodecFactory {

    @Override
    public ProtocolCodec createCodec() {
        return new ProtobufCodec();
    }

}
