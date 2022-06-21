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

import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Response.Status;

public abstract class AbstractCodec implements ProtocolCodec {

    @Override
    public Request encode(Call call, Class<?> paramType) throws CodecException {
        Request request = new Request();
        Headers headers = request.headers();
        headers.add(Call.GROUP_KEY, call.getGroup());
        headers.add(Call.SERVICE_KEY, call.getService());
        headers.add(Call.METHOD_KEY, call.getMethod());
        headers.add(Call.CODEC_KEY, codecId());
        headers.setAll(call.attach());

        request.setTimeout(call.getTimeout());
        request.setContent(write(call.getParameter(), paramType));
        return request;
    }

    @Override
    public Call decode(Request request, Class<?> paramType) throws CodecException {
        Call call = new Call();
        Headers headers = request.headers();
        call.setGroup(headers.get(Call.GROUP_KEY));
        call.setService(headers.get(Call.SERVICE_KEY));
        call.setMethod(headers.get(Call.METHOD_KEY));
        call.setTimeout(request.getTimeout());
        call.attach().putAll(headers);
        call.setParameter(read(request.getContent(), paramType));
        return call;
    }

    @Override
    public Response encode(Reply reply, Class<?> returnType) throws CodecException {
        Response response = new Response();
        response.headers().setAll(reply.attach());
        response.headers().add(Reply.CODEC_KEY, codecId());

        response.setStatus(Status.SUCCESS);
        response.setContent(write(reply.getData(), returnType));
        return response;
    }

    @Override
    public Reply decode(Response response, Class<?> returnType) throws CodecException {
        Reply reply = new Reply();
        Headers headers = response.headers();
        if (!headers.isEmpty()) {
            reply.attach().putAll(headers);
        }
        Object data = read(response.getContent(), returnType);
        reply.setData(data);
        return reply;
    }

    protected abstract Object read(byte[] paramBytes, Class<?> paramType);

    protected abstract byte[] write(Object parameter, Class<?> paramType);

}
