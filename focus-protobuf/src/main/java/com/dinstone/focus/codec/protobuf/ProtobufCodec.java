package com.dinstone.focus.codec.protobuf;

import java.lang.reflect.Method;

import com.dinstone.focus.codec.AbstractCodec;
import com.dinstone.focus.codec.CodecException;
import com.google.protobuf.MessageLite;

public class ProtobufCodec extends AbstractCodec {

    @Override
    public String codecId() {
        return "boen/protobuf";
    }

    @Override
    protected Object read(byte[] paramBytes, Class<?> paramType) {
        if (paramBytes == null) {
            return null;
        }
        try {
            Method m = paramType.getMethod("parseFrom", byte[].class);
            return m.invoke(null, paramBytes);
        } catch (Exception e) {
            throw new CodecException("read parameter error", e);
        }
    }

    @Override
    protected byte[] write(Object parameter, Class<?> paramType) {
        if (parameter instanceof MessageLite) {
            return ((MessageLite) parameter).toByteArray();
        }
        throw new CodecException("unsported parameter type");
    }

}
