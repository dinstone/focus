package com.dinstone.focus.codec;

import com.dinstone.focus.rpc.Call;
import com.dinstone.focus.rpc.Reply;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;

public interface Codec {

    public void encode(Request request, Call call) throws CodecException;

    public void encode(Response response, Reply reply) throws CodecException;

    public Call decode(Request request) throws CodecException;

    public Reply decode(Response response) throws CodecException;

}
