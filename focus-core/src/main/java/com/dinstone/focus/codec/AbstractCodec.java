/*
 * Copyright (C) 2019~2022 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dinstone.focus.codec;

import com.dinstone.focus.protocol.Attach;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Response.Status;

public abstract class AbstractCodec implements ProtocolCodec {

    @Override
    public Request encode(Call call, Request request) throws CodecException {
        // Request request = new Request();
        Headers headers = request.headers();
        headers.add("rpc.call.group", call.getGroup());
        headers.add("rpc.call.service", call.getService());
        headers.add("rpc.call.method", call.getMethod());
        headers.add("rpc.call.timeout", "" + call.getTimeout());
        headers.add("rpc.call.codec", codecId());
        // headers.add(call.attach());

        request.setTimeout(call.getTimeout());
        request.setContent(write(call.getParameter(), call.getParamType()));
        return request;
    }

    @Override
    public Call decode(Request request, Call call) throws CodecException {
        Headers headers = request.headers();
        call.setGroup(headers.get("rpc.call.group"));
        call.setService(headers.get("rpc.call.service"));
        call.setMethod(headers.get("rpc.call.method"));
        call.attach(new Attach(headers));
        call.setTimeout(request.getTimeout());
        call.setParameter(read(request.getContent(), call.getParamType()));
        return call;
    }

    @Override
    public Response encode(Reply reply, Response response) throws CodecException {
        // Response response = new Response();
        // response.headers().setAll(reply.attach());
        response.headers().add("rpc.call.codec", codecId());

        response.setStatus(Status.SUCCESS);
        response.setContent(write(reply.getData(), reply.getDataType()));
        return response;
    }

    @Override
    public Reply decode(Response response, Reply reply) throws CodecException {
        // Reply reply = readReply(response.getContent());
        Headers headers = response.headers();
        if (headers != null && !headers.isEmpty()) {
            reply.attach(new Attach(headers));
        }
        Object data = read(response.getContent(), reply.getDataType());
        reply.setData(data);
        return reply;
    }

    protected abstract Object read(byte[] paramBytes, Class<?> paramType);

    protected abstract byte[] write(Object parameter, Class<?> paramType);

}
