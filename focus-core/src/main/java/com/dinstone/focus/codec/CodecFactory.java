package com.dinstone.focus.codec;

import com.dinstone.focus.SchemaFactory;

public interface CodecFactory extends SchemaFactory {

    public Codec createCodec();
}
